package kscience.communicator.zmq.client

import co.touchlab.stately.collections.IsoArrayDeque
import co.touchlab.stately.collections.IsoMutableMap
import kscience.communicator.api.Payload
import kscience.communicator.api.Transport
import kscience.communicator.zmq.platform.*
import kscience.communicator.zmq.util.sendMsg

internal const val NEW_QUERIES_QUEUE_UPDATE_INTERVAL = 1000

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
    internal val ctx: ZmqContext = ZmqContext(),
    internal val mainDealer: ZmqSocket = ctx.createDealerSocket(),
    internal val identity: UniqueID = UniqueID(),
    internal val newQueriesQueue: IsoArrayDeque<Query> = IsoArrayDeque(),
    internal val specQueriesQueue: IsoArrayDeque<SpecQuery> = IsoArrayDeque(),
    internal val queriesInWork: IsoMutableMap<UniqueID, ResultCallback> = IsoMutableMap(),
    internal val specQueriesInWork: IsoMutableMap<UniqueID, SpecCallback> = IsoMutableMap(),
    internal val forwardSockets: IsoMutableMap<String, ZmqSocket> = IsoMutableMap(),
    internal val reactor: ZmqLoop = ZmqLoop(ctx),
) : Transport {
    public override suspend fun respond(address: String, name: String, payload: Payload): Payload =
        respondImpl(address, name, payload)

    public constructor() : this(ctx = ZmqContext())

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
        reactor.close()
        mainDealer.close()
        ctx.close()
    }
}

internal expect suspend fun ZmqTransport.respondImpl(
    address: String,
    name: String,
    payload: ByteArray
): ByteArray

internal fun initClientBlocking(client: ZmqTransport): Unit = with(client) {
    reactor.addTimer(NEW_QUERIES_QUEUE_UPDATE_INTERVAL, 0, ZmqLoop.Argument(this)) {
        it.value.handleQueriesQueue()
        it.value.handleSpecQueue()
        0
    }

    reactor.addReader(mainDealer, ZmqLoop.Argument(Unit)) { _ -> 0 }
    reactor.start()
}

internal expect fun initClient(client: ZmqTransport)

private fun ZmqTransport.handleQueriesQueue() {
    val query = newQueriesQueue.removeLastOrNull() ?: return
    println("Making query ${query.functionName}")
    val id = UniqueID()
    queriesInWork[id] = query.callback

    sendMsg(getForwardSocket(query.address)) {
        +identity
        +"QUERY"
        +id
        +query.arg
        +query.functionName.encodeToByteArray()
    }
}

private fun ZmqTransport.getForwardSocket(address: String): ZmqSocket {
    val existing = forwardSockets[address]
    if (existing != null) return existing
    val forwardSocket = ctx.createDealerSocket()
    forwardSocket.setIdentity(identity.bytes)
    forwardSocket.connect("tcp://$address")

    reactor.addReader(forwardSocket, ZmqLoop.Argument(ResultHandlerArg(forwardSocket, this))) {
        it.value.client.handleResult(it.value)
        0
    }

    forwardSockets[address] = forwardSocket
    return forwardSocket
}

private fun ZmqTransport.handleSpecQueue() {
    val specQuery = specQueriesQueue.removeLastOrNull() ?: return
    println("Making spec query ${specQuery.functionName}")
    val id = UniqueID()
    specQueriesInWork[id] = specQuery.callback

    sendMsg(getForwardSocket(specQuery.address)) {
        +identity
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
        println("Handler can't find callback in queriesInWork queue")
        return
    }

    callback.onResult(result)
}
