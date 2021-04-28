package space.kscience.communicator.zmq.server

import co.touchlab.stately.collections.IsoArrayDeque
import co.touchlab.stately.collections.IsoMutableList
import co.touchlab.stately.collections.IsoMutableMap
import co.touchlab.stately.isolate.StateRunner
import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.*
import mu.KLogger
import mu.KotlinLogging
import space.kscience.communicator.api.ClientEndpoint
import space.kscience.communicator.api.FunctionSpec
import space.kscience.communicator.api.IntCoder
import space.kscience.communicator.api.PayloadFunction
import space.kscience.communicator.zmq.Protocol
import space.kscience.communicator.zmq.platform.ZmqContext
import space.kscience.communicator.zmq.platform.ZmqLoop
import space.kscience.communicator.zmq.platform.ZmqSocket
import space.kscience.communicator.zmq.util.DaemonStateRunner
import space.kscience.communicator.zmq.util.runAsync
import space.kscience.communicator.zmq.util.sendMsg

public class ZmqWorker private constructor(
    internal val proxy: ClientEndpoint,
    private val stateRunner: StateRunner = DaemonStateRunner(),
    internal val serverFunctions: IsoMutableMap<String, Pair<PayloadFunction, FunctionSpec<*, *>>>,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    internal val workerScope: CoroutineScope = CoroutineScope(workerDispatcher + SupervisorJob()),
    private val ctx: ZmqContext = ZmqContext(),
    internal val repliesQueue: IsoArrayDeque<Response> = IsoArrayDeque(stateRunner),
    internal val editFunctionQueriesQueue: IsoArrayDeque<WorkerEditFunctionQuery> = IsoArrayDeque(stateRunner),
    internal val frontend: ZmqSocket = ctx.createDealerSocket(),
    private val reactor: ZmqLoop = ZmqLoop(ctx),
    private val active: IsoMutableList<Int> = IsoMutableList(stateRunner) { mutableListOf(0) },
    internal val logger: KLogger = KotlinLogging.logger("ZmqWorker(${proxy.host}:${proxy.port})"),
) : Closeable {
    public constructor(
        proxy: ClientEndpoint,
        serverFunctions: MutableMap<String, Pair<PayloadFunction, FunctionSpec<*, *>>>,
        workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
        workerScope: CoroutineScope = CoroutineScope(workerDispatcher + SupervisorJob()),
        stateRunner: StateRunner = DaemonStateRunner(),
    ) : this(
        proxy = proxy,
        stateRunner = stateRunner,
        serverFunctions = IsoMutableMap(stateRunner) { serverFunctions },
        workerDispatcher = workerDispatcher,
        workerScope = workerScope,
    )

    init {
        runAsync(this) { start() }
    }

    /**
     * Stops and disposes this worker.
     */
    public override fun close() {
        workerScope.cancel("Proxy is being stopped.")
        serverFunctions.dispose()
        ctx.close()
    }

    internal suspend fun start() {
        frontend.connect("tcp://${proxy.host}:${proxy.port + 1}")

        frontend.sendMsg {
            +Protocol.Worker.Register
            +IntCoder.encode(serverFunctions.size)

            serverFunctions.mapValues { it.value.second }.forEach {
                +it.key
                +it.value.argumentCoder.identity
                +it.value.resultCoder.identity
            }
        }

        reactor.addReader(
            frontend,
            ZmqLoop.Argument(this),
        ) {
            it.value.handleWorkerFrontend()
            active[0]
        }

        reactor.addTimer(
            1,
            0,
            ZmqLoop.Argument(this),
        ) {
            it.value.handleReplyQueue()
            active[0]
        }

        reactor.addTimer(
            1,
            0,
            ZmqLoop.Argument(this),
        ) {
            it.value.handleEditFunctionQueue()
            active[0]
        }

        reactor.start()
    }

    public override fun toString(): String = "ZmqWorker(${proxy.host}:${proxy.port}))"
}

internal sealed class WorkerEditFunctionQuery

private class WorkerRegisterFunctionQuery(
    val name: String,
    val function: PayloadFunction,
    val spec: FunctionSpec<*, *>,
) : WorkerEditFunctionQuery()

private class WorkerUnregisterFunctionQuery(val name: String) : WorkerEditFunctionQuery()

internal sealed class WorkerResponse

internal class WorkerResponseResult(val clientIdentity: ByteArray, val queryID: ByteArray, val resultBytes: ByteArray) :
    WorkerResponse()

internal class WorkerResponseException(
    val clientIdentity: ByteArray,
    val queryID: ByteArray,
    val exceptionMessage: String,
) : WorkerResponse()

private fun ZmqWorker.handleReplyQueue() {
    while (true) {
        val reply = repliesQueue.removeLastOrNull() ?: break

        frontend.sendMsg {
            when (reply) {
                is ResponseResult -> {
                    +Protocol.Response.Result
                    +reply.queryID
                    +reply.resultBytes
                }

                is ResponseException -> {
                    +Protocol.Response.Exception
                    +reply.queryID
                    +reply.exceptionMessage
                }
            }
        }
    }
}

private fun ZmqWorker.handleEditFunctionQueue() {
    while (true) {
        val editFunctionMessage = editFunctionQueriesQueue.removeLastOrNull() ?: break

        when (editFunctionMessage) {
            is WorkerRegisterFunctionQuery -> serverFunctions[editFunctionMessage.name] =
                editFunctionMessage.function to editFunctionMessage.spec

            is WorkerUnregisterFunctionQuery -> serverFunctions -= editFunctionMessage.name
        }
    }
}
