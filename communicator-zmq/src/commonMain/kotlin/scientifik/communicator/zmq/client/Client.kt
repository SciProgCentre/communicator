package scientifik.communicator.zmq.client

import scientifik.communicator.api.Payload
import scientifik.communicator.api.log
import scientifik.communicator.zmq.platform.*


private const val NEW_QUERIES_QUEUE_UPDATE_INTERVAL = 1

typealias ResultHandler = (ByteArray) -> Unit
typealias ErrorHandler = (Throwable) -> Unit

class ResultCallback(val onResult: ResultHandler, val onError: ErrorHandler)

internal class Query(
        val functionName: String,
        val address: String,
        val arg: Payload,
        val callback: ResultCallback
)

private class ClientContext(
        val ctx: ZMQContext,
        val identity: QueryID,
        // В эту очередь попадают запросы при вызове remoteFunction.invoke()
        val newQueriesQueue: ConcurrentQueue<Query>,
        // В этот словарь попадают запросы, которые уже отправлены на сервер и сервер ответил о том, что он получил их
        val queriesInWork: HashMap<QueryID, ResultCallback>,
        val forwardSockets: HashMap<String, ZMQSocket>,
        val reactor: ZMQLoop
)

/**
 * Принимает запросы о вызове удаленной функции из любых потоков и вызывает коллбек при получении результата
 */
internal class Client {

    private val newQueriesQueue = ConcurrentQueue<Query>()

    fun makeQuery(query: Query) {
        log("Adding query ${query.functionName} to the internal queue")
        newQueriesQueue.add(query)
    }

    init {
        runInBackground {
            val ctx = ZMQContext()
            val clientContext = ClientContext(
                    ctx = ctx,
                    identity = QueryID(),
                    newQueriesQueue = newQueriesQueue,
                    queriesInWork = HashMap(),
                    forwardSockets = HashMap(),
                    reactor = ZMQLoop(ctx)
            )
            with(clientContext) {
                reactor.addTimer(NEW_QUERIES_QUEUE_UPDATE_INTERVAL, 0, { _, _, arg ->
                    (arg as ClientContext).handleQueue()
                    0
                }, clientContext)
                reactor.addReader(ctx.createDealerSocket(), { _, _, _ -> 0 }, Unit)
                reactor.start()
            }
        }
    }
}

private fun ClientContext.handleQueue() {
    while (true) {
        val query = newQueriesQueue.poll() ?: break
        log("Making query ${query.functionName}")
        val id = QueryID()
        queriesInWork[id] = query.callback
        val forwardSocket = getForwardSocket(query.address)
        sendQuery(forwardSocket, query, id)
    }
}

private fun ClientContext.getForwardSocket(
        address: String
): ZMQSocket {
    val existing = forwardSockets[address]
    if (existing != null) return existing
    val forwardSocket = ctx.createDealerSocket()
    forwardSocket.setIdentity(identity.bytes)
    forwardSocket.connect("tcp://$address")
    reactor.addReader(forwardSocket, { _, _, arg ->
        arg as ResultHandlerArg
        arg.clientContext.handleResult(arg)
        0
    }, ResultHandlerArg(forwardSocket, this))
    forwardSockets[address] = forwardSocket
    return forwardSocket
}

private fun sendQuery(socket: ZMQSocket, query: Query, queryID: QueryID) {
    val msg = ZMQMsg()
    msg.add(queryID.bytes)
    msg.add(query.functionName.encodeToByteArray())
    msg.add(query.arg)
    msg.send(socket)
}

private class ResultHandlerArg(
        val socket: ZMQSocket,
        val clientContext: ClientContext
)

private fun ClientContext.handleResult(arg: ResultHandlerArg) {
    log("Handling result")
    val msg: ZMQMsg = arg.socket.recvMsg()
    val queryID = QueryID(msg.pop().data)
    val result = msg.pop().data
    log("Got result to the query [$queryID]: $result")
    val callback = queriesInWork[queryID]
    if (callback == null) {
        log("ERROR: handler can't find callback in waitingQueries queue")
        return
    }
    callback.onResult(result)
}