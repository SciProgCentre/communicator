package space.kscience.communicator.zmq.server

import co.touchlab.stately.collections.IsoArrayDeque
import co.touchlab.stately.collections.IsoMutableList
import co.touchlab.stately.collections.IsoMutableMap
import co.touchlab.stately.isolate.StateRunner
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import space.kscience.communicator.api.Codec
import space.kscience.communicator.api.PayloadFunction
import space.kscience.communicator.api.TransportServer
import space.kscience.communicator.zmq.Protocol
import space.kscience.communicator.zmq.platform.ZmqContext
import space.kscience.communicator.zmq.platform.ZmqLoop
import space.kscience.communicator.zmq.platform.ZmqSocket
import space.kscience.communicator.zmq.util.DaemonStateRunner
import space.kscience.communicator.zmq.util.runAsync
import space.kscience.communicator.zmq.util.sendMsg

/**
 * Implements transport server with ZeroMQ-based machinery. Associated client transport is
 * [space.kscience.communicator.zmq.client.ZmqTransportClient].
 *
 * The recommended protocol identifier is `ZMQ`.
 */
public class ZmqTransportServer private constructor(
    override val port: Int,
    private val stateRunner: StateRunner = DaemonStateRunner(),

    internal val serverFunctions: IsoMutableMap<String, Triple<PayloadFunction, Codec<*>, Codec<*>>> =
        IsoMutableMap(stateRunner) { HashMap() },

    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    internal val workerScope: CoroutineScope = CoroutineScope(workerDispatcher + SupervisorJob()),
    private val ctx: ZmqContext = ZmqContext(),
    internal val repliesQueue: IsoArrayDeque<Response> = IsoArrayDeque(stateRunner),
    internal val editFunctionQueriesQueue: IsoArrayDeque<EditFunctionQuery> = IsoArrayDeque(stateRunner),
    internal val frontend: ZmqSocket = ctx.createRouterSocket(),
    private val reactor: ZmqLoop = ZmqLoop(ctx),
    private val active: IsoMutableList<Int> = IsoMutableList(stateRunner) { mutableListOf(0) },
    internal val logger: KLogger = KotlinLogging.logger("ZmqTransportServer($port)"),
) : TransportServer {
    public constructor (port: Int) : this(port, stateRunner = DaemonStateRunner())

    internal fun start() {
        logger.info { "Starting ZmqTransportServer bound to $port." }
        frontend.bind("tcp://127.0.0.1:${port}")

        reactor.addReader(frontend, ZmqLoop.Argument(this)) {
            it.value.handleFrontend()
            active[0]
        }

        reactor.addTimer(1, 0, ZmqLoop.Argument(this)) {
            it.value.handleReplyQueue()
            active[0]
        }

        reactor.addTimer(1, 0, ZmqLoop.Argument(this)) {
            it.value.handleEditFunctionQueue()
            active[0]
        }

        logger.info { "Starting event loop." }
        reactor.start()
    }

    override fun close() {
        logger.info { "Stopping and cleaning up." }
        active[0] = -1
        workerScope.cancel("Transport server is being stopped.")
        repliesQueue.dispose()
        editFunctionQueriesQueue.dispose()
        active.dispose()
        ctx.close()
    }

    init {
        runAsync(this) { start() }
    }

    override fun register(
        name: String,
        function: PayloadFunction,
        argumentCodec: Codec<*>,
        resultCodec: Codec<*>,
    ): Unit = editFunctionQueriesQueue.addFirst(RegisterFunctionQuery(name, function, argumentCodec, resultCodec))

    override fun unregister(name: String): Unit = editFunctionQueriesQueue.addFirst(UnregisterFunctionQuery(name))
    override fun toString(): String = "ZmqTransportServer($port)"
}

internal sealed class EditFunctionQuery

private class RegisterFunctionQuery(
    val name: String,
    val function: PayloadFunction,
    val argumentCodec: Codec<*>,
    val resultCodec: Codec<*>,
) :
    EditFunctionQuery()

private class UnregisterFunctionQuery(val name: String) : EditFunctionQuery()

internal sealed class Response

internal class ResponseResult(val clientIdentity: ByteArray, val queryID: ByteArray, val resultBytes: ByteArray) :
    Response()

internal class ResponseException(val clientIdentity: ByteArray, val queryID: ByteArray, val exceptionMessage: String) :
    Response()

private fun ZmqTransportServer.handleReplyQueue() {
    while (true) {
        val reply = repliesQueue.removeLastOrNull() ?: break

        frontend.sendMsg {
            when (reply) {
                is ResponseResult -> {
                    +reply.clientIdentity
                    +Protocol.Response.Result
                    +reply.queryID
                    +reply.resultBytes
                }

                is ResponseException -> {
                    +reply.clientIdentity
                    +Protocol.Response.Exception
                    +reply.queryID
                    +reply.exceptionMessage
                }
            }
        }
    }
}

private fun ZmqTransportServer.handleEditFunctionQueue() {
    while (true) {
        val editFunctionMessage = editFunctionQueriesQueue.removeLastOrNull() ?: break

        when (editFunctionMessage) {
            is RegisterFunctionQuery -> this@handleEditFunctionQueue.serverFunctions[editFunctionMessage.name] =
                Triple(editFunctionMessage.function, editFunctionMessage.argumentCodec, editFunctionMessage.resultCodec)

            is UnregisterFunctionQuery -> this@handleEditFunctionQueue.serverFunctions -= editFunctionMessage.name
        }
    }
}
