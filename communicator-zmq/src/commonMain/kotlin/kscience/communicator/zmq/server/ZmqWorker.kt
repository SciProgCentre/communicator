package kscience.communicator.zmq.server

import co.touchlab.stately.collections.IsoArrayDeque
import co.touchlab.stately.collections.IsoMutableMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.io.Closeable
import kscience.communicator.api.Endpoint
import kscience.communicator.api.FunctionSpec
import kscience.communicator.api.IntCoder
import kscience.communicator.api.PayloadFunction
import kscience.communicator.zmq.Protocol
import kscience.communicator.zmq.platform.ZmqContext
import kscience.communicator.zmq.platform.ZmqLoop
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg

public class ZmqWorker private constructor(
    private val proxy: Endpoint,
    internal val serverFunctions: IsoMutableMap<String, PayloadFunction>,
    internal val serverFunctionSpecs: IsoMutableMap<String, FunctionSpec<*, *>>,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    internal val workerScope: CoroutineScope = CoroutineScope(workerDispatcher + SupervisorJob()),
    private val ctx: ZmqContext = ZmqContext(),
    internal val repliesQueue: IsoArrayDeque<Response> = IsoArrayDeque(),
    internal val editFunctionQueriesQueue: IsoArrayDeque<WorkerEditFunctionQuery> = IsoArrayDeque(),
    internal val frontend: ZmqSocket = ctx.createDealerSocket(),
    private val reactor: ZmqLoop = ZmqLoop(ctx)
) : Closeable {
    public constructor(
        proxy: Endpoint,
        serverFunctions: MutableMap<String, PayloadFunction>,
        serverFunctionSpecs: MutableMap<String, FunctionSpec<*, *>>,
        workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
        workerScope: CoroutineScope = CoroutineScope(workerDispatcher + SupervisorJob()),
    ) : this(
        proxy,
        IsoMutableMap { serverFunctions },
        IsoMutableMap { serverFunctionSpecs },
        workerDispatcher,
        workerScope,
    )

    init {
        initWorker(this)
    }

    public override fun close() {
        frontend.close()
        ctx.close()
    }

    internal fun start() {
        frontend.connect("tcp://${proxy.host}:${proxy.port + 1}")

        sendMsg(frontend) {
            +Protocol.Worker.Register
            +IntCoder.encode(serverFunctions.size)

            serverFunctionSpecs.forEach {
                +it.key
                +it.value.argumentCoder.identity
                +it.value.resultCoder.identity
            }
        }

        reactor.addReader(
            frontend,
            ZmqLoop.Argument(this),
        ) {
            handleWorkerFrontend(it.value)
            0
        }

        reactor.addTimer(
            1,
            0,
            ZmqLoop.Argument(this),
        ) {
            handleReplyQueue(it.value)
            0
        }

        reactor.addTimer(
            1,
            0,
            ZmqLoop.Argument(this),
        ) {
            handleEditFunctionQueue(it.value)
            0
        }

        reactor.start()
    }
}

internal expect fun initWorker(worker: ZmqWorker)

internal sealed class WorkerEditFunctionQuery

private class WorkerRegisterFunctionQuery(
    val name: String,
    val function: PayloadFunction,
    val spec: FunctionSpec<*, *>
) : WorkerEditFunctionQuery()

private class WorkerUnregisterFunctionQuery(val name: String) : WorkerEditFunctionQuery()

internal sealed class WorkerResponse

internal class WorkerResponseResult(val clientIdentity: ByteArray, val queryID: ByteArray, val resultBytes: ByteArray) :
    WorkerResponse()

internal class WorkerResponseException(
    val clientIdentity: ByteArray,
    val queryID: ByteArray,
    val exceptionMessage: String
) : WorkerResponse()

private fun handleReplyQueue(arg: ZmqWorker): Unit = with(arg) {
    while (true) {
        val reply = repliesQueue.removeLastOrNull() ?: break

        sendMsg(frontend) {
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

private fun handleEditFunctionQueue(arg: ZmqWorker): Unit = with(arg) {
    while (true) {
        val editFunctionMessage = editFunctionQueriesQueue.removeLastOrNull() ?: break

        when (editFunctionMessage) {
            is WorkerRegisterFunctionQuery -> {
                arg.serverFunctions[editFunctionMessage.name] = editFunctionMessage.function
                arg.serverFunctionSpecs[editFunctionMessage.name] = editFunctionMessage.spec
            }

            is WorkerUnregisterFunctionQuery -> arg.serverFunctions.remove(editFunctionMessage.name)
        }
    }
}
