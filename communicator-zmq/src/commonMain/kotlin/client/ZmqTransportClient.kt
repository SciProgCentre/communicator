package space.kscience.communicator.zmq.client

import co.touchlab.stately.collections.IsoArrayDeque
import co.touchlab.stately.collections.IsoMutableList
import co.touchlab.stately.collections.IsoMutableMap
import co.touchlab.stately.isolate.StateRunner
import mu.KLogger
import mu.KotlinLogging
import space.kscience.communicator.api.Payload
import space.kscience.communicator.api.TransportClient
import space.kscience.communicator.zmq.Protocol
import space.kscience.communicator.zmq.platform.UniqueID
import space.kscience.communicator.zmq.platform.ZmqContext
import space.kscience.communicator.zmq.platform.ZmqLoop
import space.kscience.communicator.zmq.platform.ZmqSocket
import space.kscience.communicator.zmq.util.DaemonStateRunner
import space.kscience.communicator.zmq.util.runAsync
import space.kscience.communicator.zmq.util.sendMsg

internal const val NEW_QUERIES_QUEUE_UPDATE_INTERVAL = 1

internal class ResultCallback(val onResult: (ByteArray) -> Unit, val onError: (Throwable) -> Unit)

internal class Query(
    val functionName: String,
    val host: String,
    val port: Int,
    val arg: Payload,
    val callback: ResultCallback,
)

internal class SpecCallback(val onSpecFound: (String, String) -> Unit, val onSpecNotFound: () -> Unit)

internal class SpecQuery(
    val functionName: String,
    val host: String,
    val port: Int,
    val callback: SpecCallback,
)

/**
 * Implements transport with ZeroMQ-based machinery. Associated server transport is
 * [space.kscience.communicator.zmq.server.ZmqTransportServer].
 *
 * The recommended protocol identifier is `ZMQ`.
 */
public class ZmqTransportClient private constructor(
    internal val ctx: ZmqContext = ZmqContext(),
    private val stateRunner: StateRunner = DaemonStateRunner(),
    internal val identity: UniqueID = UniqueID(),
    private val identityHash: Int = identity.hashCode(),
    internal val newQueriesQueue: IsoArrayDeque<Query> = IsoArrayDeque(stateRunner),
    internal val specQueriesQueue: IsoArrayDeque<SpecQuery> = IsoArrayDeque(stateRunner),
    internal val queriesInWork: IsoMutableMap<UniqueID, ResultCallback> = IsoMutableMap(stateRunner),
    internal val specQueriesInWork: IsoMutableMap<UniqueID, SpecCallback> = IsoMutableMap(stateRunner),
    internal val forwardSockets: IsoMutableMap<Pair<String, Int>, ZmqSocket> = IsoMutableMap(stateRunner),
    internal val reactor: ZmqLoop = ZmqLoop(ctx),
    internal val active: IsoMutableList<Int> = IsoMutableList(stateRunner) { mutableListOf(0) },
    internal val logger: KLogger = KotlinLogging.logger("ZmqTransport($identityHash)"),
) : TransportClient {
    public constructor() : this(ctx = ZmqContext())

    init {
        runAsync(this) { start() }
    }

    public override suspend fun respond(host: String, port: Int, name: String, payload: Payload): Payload =
        respondImpl(host, port, name, payload)


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

    public override fun toString(): String = "ZmqTransport(${identityHash})"
}

internal expect suspend fun ZmqTransportClient.respondImpl(
    host: String,
    port: Int,
    name: String,
    payload: ByteArray,
): ByteArray

private fun ZmqTransportClient.handleQueriesQueue() {
    val query = newQueriesQueue.removeLastOrNull() ?: return
    logger.info { "Making query ${query.functionName}." }
    val id = UniqueID()
    queriesInWork[id] = query.callback

    getForwardSocket(query.host , query.port).sendMsg {
        +Protocol.Query
        +id
        +query.arg
        +query.functionName.encodeToByteArray()
    }
}

private fun ZmqTransportClient.getForwardSocket(host: String, port: Int): ZmqSocket {
    val existing = forwardSockets[host to port]
    if (existing != null) return existing
    logger.info { "Opening a new socket connected to $host:$port." }
    val forwardSocket = ctx.createDealerSocket()
    forwardSocket.setIdentity(identity.bytes)
    forwardSocket.connect("tcp://$host:$port")

    reactor.addReader(forwardSocket, ZmqLoop.Argument(ForwardSocketHandlerArg(forwardSocket, this))) {
        handleForwardSocket(it.value)
        active[0]
    }

    forwardSockets[host to port] = forwardSocket
    return forwardSocket
}

private fun ZmqTransportClient.handleSpecQueue() {
    val specQuery = specQueriesQueue.removeLastOrNull() ?: return
    logger.info { "Making spec query ${specQuery.functionName}." }
    val id = UniqueID()
    specQueriesInWork[id] = specQuery.callback

    getForwardSocket(specQuery.host, specQuery.port).sendMsg {
        +Protocol.Coder.IdentityQuery
        +id
        +specQuery.functionName.encodeToByteArray()
    }
}
