package kscience.communicator.zmq.client

import co.touchlab.stately.collections.IsoArrayDeque
import co.touchlab.stately.collections.IsoMutableMap
import co.touchlab.stately.isolate.IsolateState
import kotlinx.io.use
import kscience.communicator.api.Payload
import kscience.communicator.api.Transport
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

public class ZmqTransport private constructor(
    internal val ctx: IsolateState<ZmqContext> = IsolateState(::ZmqContext),
    internal val mainDealer: IsolateState<ZmqSocket> = IsolateState { ctx.access(ZmqContext::createDealerSocket) },
    internal val identity: UniqueID = UniqueID(),
    internal val newQueriesQueue: IsoArrayDeque<Query> = IsoArrayDeque(),
    internal val specQueriesQueue: IsoArrayDeque<SpecQuery> = IsoArrayDeque(),
    internal val queriesInWork: IsoMutableMap<UniqueID, ResultCallback> = IsoMutableMap(),
    internal val specQueriesInWork: IsoMutableMap<UniqueID, SpecCallback> = IsoMutableMap(),
    internal val forwardSockets: IsoMutableMap<String, ZmqSocket> = IsoMutableMap(),
    internal val reactor: IsolateState<ZmqLoop> = IsolateState { ctx.access(::ZmqLoop) },
) : Transport {
    public override suspend fun respond(address: String, name: String, payload: Payload): Payload =
        respondImpl(address, name, payload)

    public constructor() : this(ctx = IsolateState(::ZmqContext))

    init {
        initClient(this)
    }

    internal fun makeQuery(query: Query) {
        println("Adding query ${query.functionName} to the internal queue")
        newQueriesQueue.addFirst(query)
    }

    public override fun close() {
        queriesInWork.dispose()
        newQueriesQueue.dispose()
        specQueriesInWork.dispose()
        reactor.access(ZmqLoop::close)
        reactor.dispose()
        mainDealer.access(ZmqSocket::close)
        mainDealer.dispose()
        ctx.access(ZmqContext::close)
        ctx.dispose()
    }
}

internal expect suspend fun ZmqTransport.respondImpl(
    address: String,
    name: String,
    payload: ByteArray
): ByteArray

internal fun initClientBlocking(client: ZmqTransport): Unit = with(client) {
    reactor.access {
        it.addTimer(
            NEW_QUERIES_QUEUE_UPDATE_INTERVAL,
            0,

            { _: Any?, arg ->
                checkNotNull(arg).value.handleQueriesQueue()
                arg.value.handleSpecQueue()
                0
            },

            ZmqLoop.Argument(this)
        )
    }

    mainDealer.access { dealer -> reactor.access { it.addReader(dealer, { _, _ -> 0 }, ZmqLoop.Argument(Unit)) } }
    reactor.access(ZmqLoop::start)
}

internal expect fun initClient(client: ZmqTransport)

private fun ZmqTransport.handleQueriesQueue() {
    val query = newQueriesQueue.removeFirstOrNull() ?: return
    println("Making query ${query.functionName}")
    val id = UniqueID()
    queriesInWork[id] = query.callback

    ZmqMsg().use {
        it.add(id.bytes)
        it.add(query.functionName.encodeToByteArray())
        it.add(query.arg)
        it.send(getForwardSocket(query.address))
    }
}

private fun ZmqTransport.getForwardSocket(address: String): ZmqSocket {
    val existing = forwardSockets[address]
    if (existing != null) return existing
    val forwardSocket = ctx.access(ZmqContext::createDealerSocket)
    forwardSocket.setIdentity(identity.bytes)
    forwardSocket.connect("tcp://$address")

    reactor.access {
        it.addReader(
            forwardSocket,

            { _, argParam ->
                checkNotNull(argParam).value.client.handleResult(argParam.value)
                0
            },

            ZmqLoop.Argument(ResultHandlerArg(forwardSocket, this))
        )
    }

    forwardSockets[address] = forwardSocket
    return forwardSocket
}

private fun ZmqTransport.handleSpecQueue() {
    val specQuery = specQueriesQueue.removeFirstOrNull() ?: return
    println("Making spec query ${specQuery.functionName}")
    val id = UniqueID()
    specQueriesInWork[id] = specQuery.callback

    sendMsg(getForwardSocket(specQuery.address)) {
        +"CODER_IDENTITY_QUERY"
        +id
        +specQuery.functionName.encodeToByteArray()
    }
}

private class ResultHandlerArg(
    val socket: ZmqSocket,
    val client: ZmqTransport
)

private fun ZmqTransport.handleResult(arg: ResultHandlerArg) {
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
