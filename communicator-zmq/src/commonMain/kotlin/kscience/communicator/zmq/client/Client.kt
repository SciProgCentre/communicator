package kscience.communicator.zmq.client

import co.touchlab.stately.collections.IsoArrayDeque
import co.touchlab.stately.collections.IsoMutableMap
import co.touchlab.stately.isolate.IsolateState
import kotlinx.io.Closeable
import kotlinx.io.use
import kscience.communicator.api.Payload
import kscience.communicator.zmq.platform.*

//import mu.KLogger
//import mu.KotlinLogging

internal const val NEW_QUERIES_QUEUE_UPDATE_INTERVAL = 1

internal typealias ResultHandler = (ByteArray) -> Unit
internal typealias ErrorHandler = (Throwable) -> Unit

internal class ResultCallback(val onResult: ResultHandler, val onError: ErrorHandler)

internal class Query(
    val functionName: String,
    val address: String,
    val arg: Payload,
    val callback: ResultCallback
)

internal class ClientState(
    val ctx: IsolateState<ZmqContext> = IsolateState(::ZmqContext),
    val mainDealer: IsolateState<ZmqSocket> = IsolateState { ctx.access(ZmqContext::createDealerSocket) },
    val identity: UniqueID = UniqueID(),
    // В эту очередь попадают запросы при вызове remoteFunction.invoke()
    val newQueriesQueue: IsoArrayDeque<Query> = IsoArrayDeque(),
    // В этот словарь попадают запросы, которые уже отправлены на сервер и сервер ответил о том, что он получил их
    val queriesInWork: IsoMutableMap<UniqueID, ResultCallback> = IsoMutableMap(),
    val forwardSockets: IsoMutableMap<String, ZmqSocket> = IsoMutableMap(),
    val reactor: ZmqLoop = ctx.access(::ZmqLoop)
) : Closeable {
    override fun close() {
        queriesInWork.dispose()
        newQueriesQueue.dispose()
        reactor.close()
        mainDealer.access(ZmqSocket::close)
        mainDealer.dispose()
        ctx.access(ZmqContext::close)
        ctx.dispose()
    }
}

/**
 * Принимает запросы о вызове удаленной функции из любых потоков и вызывает коллбек при получении результата
 */
internal class Client : Closeable {
    internal val state: ClientState = ClientState()

    init {
        initClient(state)
    }

    fun makeQuery(query: Query) {
//        log.info { "Adding query ${query.functionName} to the internal queue" }
        state.newQueriesQueue.addFirst(query)
    }

    override fun close(): Unit = state.close()
}

internal fun initClientBlocking(state: ClientState) {
    with(state) {
        reactor.addTimer(
            NEW_QUERIES_QUEUE_UPDATE_INTERVAL,
            0,

            { _: Any?, arg ->
                (arg?.value as ClientState).handleQueue()
                0
            },

            ZmqLoop.Argument(this)
        )

        mainDealer.access { reactor.addReader(it, { _, _ -> 0 }, ZmqLoop.Argument(Unit)) }
        reactor.start()
    }
}

internal expect fun initClient(state: ClientState)

private fun ClientState.handleQueue() {
    while (true) {
        val query = newQueriesQueue.removeFirstOrNull() ?: break
//        log.info { "Making query ${query.functionName}" }
        val id = UniqueID()
        queriesInWork[id] = query.callback
        sendQuery(getForwardSocket(query.address), query, id)
    }
}

private fun ClientState.getForwardSocket(address: String): ZmqSocket {
    val existing = forwardSockets[address]
    if (existing != null) return existing
    val forwardSocket = ctx.access(ZmqContext::createDealerSocket)
    forwardSocket.setIdentity(identity.bytes)
    forwardSocket.connect("tcp://$address")

    reactor.addReader(
        forwardSocket,

        { _, argParam ->
            argParam?.value as ResultHandlerArg
            argParam.value.clientContext.handleResult(argParam.value)
            0
        },

        ZmqLoop.Argument(ResultHandlerArg(forwardSocket, this))
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
//    log.info { "Handling result" }
    val msg = ZmqMsg.recvMsg(arg.socket)
    val queryID = UniqueID(msg.pop().data)
    val result = msg.pop().data
//    log.info { "Got result to the query [$queryID]: ${result.contentToString()}" }
    val callback = queriesInWork[queryID]
        ?: //        log.error { "handler can't find callback in waitingQueries queue" }
        return

    callback.onResult(result)
}
