package kscience.communicator.zmq.client

import co.touchlab.stately.collections.IsoArrayDeque
import co.touchlab.stately.collections.IsoMutableMap
import co.touchlab.stately.isolate.IsolateState
import kotlinx.io.Closeable
import kotlinx.io.use
import kscience.communicator.api.Payload
import kscience.communicator.zmq.platform.*
import kscience.communicator.zmq.util.sendMsg

internal const val NEW_QUERIES_QUEUE_UPDATE_INTERVAL = 1

internal class ResultCallback(val onResult: (ByteArray) -> Unit, val onError: (Throwable) -> Unit)

internal class Query(
    val functionName: String,
    val address: String,
    val arg: Payload,
    val callback: ResultCallback
)

internal class SpecCallback(val onSpecFound: (String, String) -> Unit, val onSpecNotFound: () -> Unit)

internal class SpecQuery(
    val functionName: String,
    val address: String,
    val callback: SpecCallback
)

internal class ClientState(
    val ctx: IsolateState<ZmqContext> = IsolateState(::ZmqContext),
    val mainDealer: IsolateState<ZmqSocket> = IsolateState { ctx.access(ZmqContext::createDealerSocket) },
    val identity: UniqueID = UniqueID(),
    val newQueriesQueue: IsoArrayDeque<Query> = IsoArrayDeque(),
    val specQueriesQueue: IsoArrayDeque<SpecQuery> = IsoArrayDeque(),
    val queriesInWork: IsoMutableMap<UniqueID, ResultCallback> = IsoMutableMap(),
    val specQueriesInWork: IsoMutableMap<UniqueID, SpecCallback> = IsoMutableMap(),
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

internal class Client : Closeable {
    internal val state: ClientState = ClientState()

    init {
        initClient(state)
    }

    fun makeQuery(query: Query) {
        println("Adding query ${query.functionName} to the internal queue")
        state.newQueriesQueue.addFirst(query)
    }

    override fun close(): Unit = state.close()
}

internal fun initClientBlocking(state: ClientState): Unit = with(state) {
    reactor.addTimer(
        NEW_QUERIES_QUEUE_UPDATE_INTERVAL,
        0,

        { _: Any?, arg ->
            checkNotNull(arg).value.handleQueriesQueue()
            arg.value.handleSpecQueue()
            0
        },

        ZmqLoop.Argument(this)
    )

    mainDealer.access { reactor.addReader(it, { _, _ -> 0 }, ZmqLoop.Argument(Unit)) }
    reactor.start()
}

internal expect fun initClient(state: ClientState)

private fun ClientState.handleQueriesQueue() {
    while (true) {
        val query = newQueriesQueue.removeFirstOrNull() ?: break
        println("Making query ${query.functionName}")
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

private fun ClientState.handleSpecQueue() {
    while (true) {
        val specQuery = specQueriesQueue.removeFirstOrNull() ?: break
        println("Making spec query ${specQuery.functionName}")
        val id = UniqueID()
        specQueriesInWork[id] = specQuery.callback
        sendMsg(getForwardSocket(specQuery.address)) {
            +"CODER_IDENTITY_QUERY"
            +id
            +specQuery.functionName.encodeToByteArray()
        }
    }
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
    println("Handling result")
    val msg = ZmqMsg.recvMsg(arg.socket)
    val queryID = UniqueID(msg.pop().data)
    val result = msg.pop().data
    println("Got result to the query [$queryID]: ${result.contentToString()}")

    val callback = queriesInWork[queryID] ?: run {
        println("handler can't find callback in waitingQueries queue")
        return
    }

    callback.onResult(result)
}
