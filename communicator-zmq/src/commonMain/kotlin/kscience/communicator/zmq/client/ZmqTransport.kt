package kscience.communicator.zmq.client

import co.touchlab.stately.collections.IsoArrayDeque
import co.touchlab.stately.collections.IsoMutableList
import co.touchlab.stately.collections.IsoMutableMap
import kscience.communicator.api.Payload
import kscience.communicator.api.Transport
import kscience.communicator.zmq.Protocol
import kscience.communicator.zmq.platform.UniqueID
import kscience.communicator.zmq.platform.ZmqContext
import kscience.communicator.zmq.platform.ZmqLoop
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg
import mu.KLogger
import mu.KotlinLogging

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

/**
 * Implements transport with ZeroMQ-based machinery. Associated server transport is
 * [kscience.communicator.zmq.server.ZmqTransportServer].
 *
 * The recommended protocol identifier is `ZMQ`.
 */
public class ZmqTransport private constructor(
    internal val ctx: ZmqContext = ZmqContext(),
    internal val identity: UniqueID = UniqueID(),
    internal val identityHash: Int = identity.hashCode(),
    internal val newQueriesQueue: IsoArrayDeque<Query> = IsoArrayDeque(),
    internal val specQueriesQueue: IsoArrayDeque<SpecQuery> = IsoArrayDeque(),
    internal val queriesInWork: IsoMutableMap<UniqueID, ResultCallback> = IsoMutableMap(),
    internal val specQueriesInWork: IsoMutableMap<UniqueID, SpecCallback> = IsoMutableMap(),
    internal val forwardSockets: IsoMutableMap<String, ZmqSocket> = IsoMutableMap(),
    internal val reactor: ZmqLoop = ZmqLoop(ctx),
    internal val active: IsoMutableList<Int> = IsoMutableList { mutableListOf(0) },
    internal val logger: KLogger = KotlinLogging.logger("ZmqTransport($identityHash)"),
) : Transport {
    public override suspend fun respond(address: String, name: String, payload: Payload): Payload =
        respondImpl(address, name, payload)

    public constructor() : this(ctx = ZmqContext())

    init {
        initClient(this)
    }

    internal fun start() {
        logger.info { "Starting client with identity $identity." }

        reactor.addTimer(NEW_QUERIES_QUEUE_UPDATE_INTERVAL, 0, ZmqLoop.Argument(this)) {
            it.value.handleQueriesQueue()
            it.value.handleSpecQueue()
            active[0]
        }

        // Adding reader to ensure reactor will be rebuilt.
        reactor.addReader(ctx.createDealerSocket(), ZmqLoop.Argument(Unit)) { active[0] }

        logger.info { "Starting event loop." }
        reactor.start()
    }

    internal fun makeQuery(query: Query) {
        logger.info { "Adding query ${query.functionName} to the internal queue." }
        newQueriesQueue.addFirst(query)
    }

    public override fun close() {
        logger.info { "Stopping and cleaning up." }
        active[0] = -1
        newQueriesQueue.dispose()
        specQueriesQueue.dispose()
        queriesInWork.dispose()
        specQueriesInWork.dispose()
        active.dispose()
        ctx.close()
    }
}

internal expect suspend fun ZmqTransport.respondImpl(address: String, name: String, payload: ByteArray): ByteArray
internal expect fun initClient(client: ZmqTransport)

private fun ZmqTransport.handleQueriesQueue() {
    val query = newQueriesQueue.removeLastOrNull() ?: return
    logger.info { "Making query ${query.functionName}." }
    val id = UniqueID()
    queriesInWork[id] = query.callback

    getForwardSocket(query.address).sendMsg {
        +Protocol.Query
        +id
        +query.arg
        +query.functionName.encodeToByteArray()
    }
}

private fun ZmqTransport.getForwardSocket(address: String): ZmqSocket {
    val existing = forwardSockets[address]
    if (existing != null) return existing
    logger.info { "Opening a new socket connected to $address." }
    val forwardSocket = ctx.createDealerSocket()
    forwardSocket.setIdentity(identity.bytes)
    forwardSocket.connect("tcp://$address")

    reactor.addReader(forwardSocket, ZmqLoop.Argument(ForwardSocketHandlerArg(forwardSocket, this))) {
        handleForwardSocket(it.value)
        active[0]
    }

    forwardSockets[address] = forwardSocket
    return forwardSocket
}

private fun ZmqTransport.handleSpecQueue() {
    val specQuery = specQueriesQueue.removeLastOrNull() ?: return
    logger.info { "Making spec query ${specQuery.functionName}." }
    val id = UniqueID()
    specQueriesInWork[id] = specQuery.callback

    getForwardSocket(specQuery.address).sendMsg {
        +Protocol.Coder.IdentityQuery
        +id
        +specQuery.functionName.encodeToByteArray()
    }
}
