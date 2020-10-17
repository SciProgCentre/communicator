package kscience.communicator.zmq.server

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kscience.communicator.api.Endpoint
import kscience.communicator.api.FunctionSpec
import kscience.communicator.api.IntCoder
import kscience.communicator.api.PayloadFunction
import kscience.communicator.zmq.platform.ZmqContext
import kscience.communicator.zmq.platform.ZmqLoop
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg

public class ZmqWorker(
    internal val proxy: Endpoint,
    internal val serverFunctions: MutableMap<String, PayloadFunction>,
    internal val serverFunctionSpecs: MutableMap<String, FunctionSpec<*, *>>
) {
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default
    private val workerScope: CoroutineScope = CoroutineScope(workerDispatcher + SupervisorJob())
    private val ctx: ZmqContext = ZmqContext()
    private val repliesQueue: Channel<Response> = Channel(BUFFERED)
    private val editFunctionQueriesQueue: Channel<WorkerEditFunctionQuery> = Channel(BUFFERED)
    internal val frontend: ZmqSocket = ctx.createDealerSocket()

    init {
        initWorker(this)
    }

    public fun close() {
        editFunctionQueriesQueue.close()
        repliesQueue.close()
        frontend.close()
        ctx.close()
    }

    internal fun start() {
        val reactor = ZmqLoop(ctx)

        reactor.addReader(
            frontend,

            { _, arg ->
                handleWorkerFrontend(checkNotNull(arg).value)
                0
            },

            ZmqLoop.Argument(
                WorkerFrontendHandlerArg(
                    workerScope,
                    frontend,
                    serverFunctions,
                    serverFunctionSpecs,
                    repliesQueue
                )
            )
        )

        reactor.addTimer(
            1,
            0,

            { _, arg ->
                handleReplyQueue(checkNotNull(arg).value)
                0
            },

            ZmqLoop.Argument(WorkerReplyQueueHandlerArg(frontend, repliesQueue))
        )

        reactor.addTimer(
            1,
            0,

            { _, arg ->
                handleEditFunctionQueue(checkNotNull(arg).value)
                0
            },

            ZmqLoop.Argument(
                WorkerEditFunctionQueueHandlerArg(
                    serverFunctions,
                    serverFunctionSpecs,
                    editFunctionQueriesQueue
                )
            )
        )

        reactor.start()
    }
}

internal expect fun initWorker(worker: ZmqWorker)

internal fun initWorkerBlocking(state: ZmqWorker) = with(state) {
    frontend.connect("tcp://${proxy.host}:${proxy.port + 1}")

    sendMsg(frontend) {
        +"WORKER_REGISTER"
        +IntCoder.encode(serverFunctions.size)
        serverFunctionSpecs.forEach {
            +it.key
            +it.value.argumentCoder.identity
            +it.value.resultCoder.identity
        }
    }

    start()
}

private sealed class WorkerEditFunctionQuery

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


private class WorkerReplyQueueHandlerArg(
    val frontend: ZmqSocket,
    val repliesQueue: Channel<Response>
)

private fun handleReplyQueue(arg: WorkerReplyQueueHandlerArg): Unit = with(arg) {
    while (true) {
        val reply = repliesQueue.poll() ?: break
        sendMsg(frontend) {
            when (reply) {
                is ResponseResult -> {
                    +"RESPONSE_RESULT"
                    +reply.queryID
                    +reply.resultBytes
                }
                is ResponseException -> {
                    +"RESPONSE_EXCEPTION"
                    +reply.queryID
                    +reply.exceptionMessage
                }
            }
        }
    }
}

private class WorkerEditFunctionQueueHandlerArg(
    val serverFunctions: MutableMap<String, PayloadFunction>,
    val serverFunctionSpecs: MutableMap<String, FunctionSpec<*, *>>,
    val editFunctionQueue: Channel<WorkerEditFunctionQuery>
)

private fun handleEditFunctionQueue(arg: WorkerEditFunctionQueueHandlerArg): Unit = with(arg) {
    while (true) {
        val editFunctionMessage = editFunctionQueue.poll() ?: break

        when (editFunctionMessage) {
            is WorkerRegisterFunctionQuery -> {
                arg.serverFunctions[editFunctionMessage.name] = editFunctionMessage.function
                arg.serverFunctionSpecs[editFunctionMessage.name] = editFunctionMessage.spec
            }
            is WorkerUnregisterFunctionQuery -> arg.serverFunctions.remove(editFunctionMessage.name)
        }
    }
}
