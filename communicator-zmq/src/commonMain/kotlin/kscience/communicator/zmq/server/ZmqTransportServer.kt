package kscience.communicator.zmq.server

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.io.use
import kscience.communicator.api.FunctionSpec
import kscience.communicator.api.PayloadFunction
import kscience.communicator.api.TransportServer
import kscience.communicator.zmq.platform.*
import kscience.communicator.zmq.util.sendMsg
import mu.KLogger
import mu.KotlinLogging

/**
 * Implements transport server with ZeroMQ-based machinery. Associated client transport is
 * [kscience.communicator.zmq.client.ZmqTransport].
 */
class ZmqTransportServer(override val port: Int) : TransportServer {
    internal val log: KLogger = KotlinLogging.logger("ZmqTransportServer")
    private val serverFunctions: MutableMap<String, PayloadFunction> = hashMapOf()
    private val serverFunctionSpecs: MutableMap<String, FunctionSpec<*, *>> = hashMapOf()
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default
    private val workerScope: CoroutineScope = CoroutineScope(workerDispatcher + SupervisorJob())
    private val ctx: ZmqContext = ZmqContext()
    private val repliesQueue: Channel<Response> = Channel(BUFFERED)
    private val editFunctionQueriesQueue: Channel<EditFunctionQuery> = Channel(BUFFERED)
    private val frontend: ZmqSocket = ctx.createRouterSocket()

    init {
        //TODO
        runInBackground({}) {
            frontend.bind("tcp://*:$port")
            start()
        }
    }

    override suspend fun register(name: String, function: PayloadFunction, spec: FunctionSpec<*, *>) {
        editFunctionQueriesQueue.send(RegisterFunctionQuery(name, function, spec))
    }

    override suspend fun unregister(name: String) {
        editFunctionQueriesQueue.send(UnregisterFunctionQuery(name))
    }

    override fun close() {
        editFunctionQueriesQueue.close()
        repliesQueue.close()
        frontend.close()
        ctx.close()
    }

    private fun start() {
        val reactor = ZmqLoop(ctx)

        reactor.addReader(
            frontend,
            { _, arg ->
                handleFrontend(arg as FrontendHandlerArg)
                0
            },
            FrontendHandlerArg(workerScope, frontend, serverFunctions, serverFunctionSpecs, repliesQueue)
        )

        reactor.addTimer(
            1,
            0,
            { _, arg ->
                handleReplyQueue(arg as ReplyQueueHandlerArg)
                0
            },
            ReplyQueueHandlerArg(frontend, repliesQueue)
        )

        reactor.addTimer(
            1,
            0,
            { _, arg ->
                handleEditFunctionQueue(arg as EditFunctionQueueHandlerArg)
                0
            },
            EditFunctionQueueHandlerArg(serverFunctions, serverFunctionSpecs, editFunctionQueriesQueue)
        )

        reactor.start()
    }
}

private sealed class EditFunctionQuery
private class RegisterFunctionQuery(val name: String, val function: PayloadFunction, val spec: FunctionSpec<*, *>) : EditFunctionQuery()
private class UnregisterFunctionQuery(val name: String) : EditFunctionQuery()

internal sealed class Response
internal class ResponseResult(val clientIdentity: ByteArray, val queryID: ByteArray, val resultBytes: ByteArray) : Response()
internal class ResponseException(val clientIdentity: ByteArray, val queryID: ByteArray, val exceptionMessage: String) : Response()


private class ReplyQueueHandlerArg(
    val frontend: ZmqSocket,
    val repliesQueue: Channel<Response>
)

private fun ZmqTransportServer.handleReplyQueue(arg: ReplyQueueHandlerArg): Unit = with(arg) {
    while (true) {
        val reply = repliesQueue.poll() ?: break
        sendMsg(frontend) {
            when (reply) {
                is ResponseResult -> {
                    +reply.clientIdentity
                    +"RESPONSE_RESULT"
                    +reply.queryID
                    +reply.resultBytes
                }
                is ResponseException -> {
                    +reply.clientIdentity
                    +"RESPONSE_EXCEPTION"
                    +reply.queryID
                    +reply.exceptionMessage
                }
            }
        }
    }
}

private class EditFunctionQueueHandlerArg(
    val serverFunctions: MutableMap<String, PayloadFunction>,
    val serverFunctionSpecs: MutableMap<String, FunctionSpec<*, *>>,
    val editFunctionQueue: Channel<EditFunctionQuery>
)

private fun handleEditFunctionQueue(arg: EditFunctionQueueHandlerArg): Unit = with(arg) {
    while (true) {
        val editFunctionMessage = editFunctionQueue.poll() ?: break

        when (editFunctionMessage) {
            is RegisterFunctionQuery -> {
                arg.serverFunctions[editFunctionMessage.name] = editFunctionMessage.function
                arg.serverFunctionSpecs[editFunctionMessage.name] = editFunctionMessage.spec
            }
            is UnregisterFunctionQuery -> arg.serverFunctions.remove(editFunctionMessage.name)
        }
    }
}
