package kscience.communicator.zmq.client

import kotlinx.coroutines.channels.Channel
import kotlinx.io.Closeable
import kotlinx.io.use
import kscience.communicator.api.Payload
import kscience.communicator.zmq.platform.*
import mu.KLogger
import mu.KotlinLogging

private const val NEW_QUERIES_QUEUE_UPDATE_INTERVAL = 1

internal typealias ResultHandler = (ByteArray) -> Unit
internal typealias ErrorHandler = (Throwable) -> Unit

internal class ResultCallback(val onResult: ResultHandler, val onError: ErrorHandler)

internal class Query(
    val functionName: String,
    val address: String,
    val arg: Payload,
    val callback: ResultCallback
)

private class ClientState(
    val ctx: ZmqContext = ZmqContext(),
    val mainDealer: ZmqSocket = ctx.createDealerSocket(),
    val identity: UniqueID = UniqueID(),
    // В эту очередь попадают запросы при вызове remoteFunction.invoke()
    val newQueriesQueue: Channel<Query> = Channel(),
    // В этот словарь попадают запросы, которые уже отправлены на сервер и сервер ответил о том, что он получил их
    val queriesInWork: MutableMap<UniqueID, ResultCallback> = hashMapOf(),
    val forwardSockets: MutableMap<String, ZmqSocket> = hashMapOf(),
    val reactor: ZmqLoop = ZmqLoop(ctx)
) : Closeable {
    val log: KLogger = KotlinLogging.logger(this::class.simpleName.orEmpty())

    override fun close() {
        newQueriesQueue.close()
        reactor.close()
        mainDealer.close()
        ctx.close()
    }
}

/**
 * Принимает запросы о вызове удаленной функции из любых потоков и вызывает коллбек при получении результата
 */
internal class Client : Closeable {
    private val log: KLogger = KotlinLogging.logger(this::class.simpleName.orEmpty())
    private val state: ClientState = ClientState()

    init {
        // TODO
        runInBackground({}) {
            with(state) {
                reactor.addTimer(
                    NEW_QUERIES_QUEUE_UPDATE_INTERVAL,
                    0,

                    { _, arg ->
                        (arg as ClientState).handleQueue()
                        0
                    },

                    this
                )

                reactor.addReader(mainDealer, { _, _ -> 0 }, Unit)
                reactor.start()
            }
        }
    }

    suspend fun makeQuery(query: Query) {
        log.info { "Adding query ${query.functionName} to the internal queue" }
        state.newQueriesQueue.send(query)
    }

    override fun close(): Unit = state.close()
}

private fun ClientState.handleQueue() {
    while (true) {
        val query = newQueriesQueue.poll() ?: break
        log.info { "Making query ${query.functionName}" }
        val id = UniqueID()
        queriesInWork[id] = query.callback
        sendQuery(getForwardSocket(query.address), query, id)
    }
}

private fun ClientState.getForwardSocket(address: String): ZmqSocket {
    val existing = forwardSockets[address]
    if (existing != null) return existing
    val forwardSocket = ctx.createDealerSocket()
    forwardSocket.setIdentity(identity.bytes)
    forwardSocket.connect("tcp://$address")

    reactor.addReader(
        forwardSocket,
        { _, arg ->
            arg as ResultHandlerArg
            arg.clientContext.handleResult(arg)
            0
        },
        ResultHandlerArg(forwardSocket, this)
    )

    forwardSockets[address] = forwardSocket
    return forwardSocket
}

private fun sendQuery(socket: ZmqSocket, query: Query, queryID: UniqueID): Unit = ZmqMsg().use {
    it.add(queryID.bytes)
    it.add(query.functionName.encodeToByteArray())
    it.add(query.arg)
    it.send(socket)
}

private class ResultHandlerArg(
    val socket: ZmqSocket,
    val clientContext: ClientState
)

private fun ClientState.handleResult(arg: ResultHandlerArg) {
    log.info { "Handling result" }
    val msg = ZmqMsg.recvMsg(arg.socket)
    val queryID = UniqueID(msg.pop().data)
    val result = msg.pop().data
    log.info { "Got result to the query [$queryID]: ${result.contentToString()}" }
    val callback = queriesInWork[queryID]

    if (callback == null) {
        log.error { "handler can't find callback in waitingQueries queue" }
        return
    }

    callback.onResult(result)
}
