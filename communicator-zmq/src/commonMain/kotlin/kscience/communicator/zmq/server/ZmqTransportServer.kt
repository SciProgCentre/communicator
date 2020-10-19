package kscience.communicator.zmq.server

import co.touchlab.stately.collections.IsoArrayDeque
import co.touchlab.stately.collections.IsoMutableMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kscience.communicator.api.FunctionSpec
import kscience.communicator.api.PayloadFunction
import kscience.communicator.api.TransportServer
import kscience.communicator.zmq.Protocol
import kscience.communicator.zmq.platform.ZmqContext
import kscience.communicator.zmq.platform.ZmqLoop
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg

/**
 * Implements transport server with ZeroMQ-based machinery. Associated client transport is
 * [kscience.communicator.zmq.client.ZmqTransport].
 */
public class ZmqTransportServer private constructor(
    public override val port: Int,
    internal val serverFunctionSpecs: IsoMutableMap<String, FunctionSpec<*, *>> = IsoMutableMap(),
    internal val serverFunctions: IsoMutableMap<String, PayloadFunction> = IsoMutableMap(),
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
    internal val workerScope: CoroutineScope = CoroutineScope(workerDispatcher + SupervisorJob()),
    private val ctx: ZmqContext = ZmqContext(),
    internal val repliesQueue: IsoArrayDeque<Response> = IsoArrayDeque(),
    internal val editFunctionQueriesQueue: IsoArrayDeque<EditFunctionQuery> = IsoArrayDeque(),
    internal val frontend: ZmqSocket = ctx.createDealerSocket(),
    private val reactor: ZmqLoop = ZmqLoop(ctx)
) : TransportServer {
    public constructor (port: Int) : this(port, serverFunctionSpecs = IsoMutableMap())

    internal fun start() {
        reactor.addReader(
            frontend,
            ZmqLoop.Argument(this),
        ) {
            it.value.handleFrontend()
            0
        }

        reactor.addTimer(
            1,
            0,
            ZmqLoop.Argument(this),
        ) {
            it.value.handleReplyQueue()
            0
        }

        reactor.addTimer(
            1,
            0,
            ZmqLoop.Argument(this),
        ) {
            it.value.handleEditFunctionQueue()
            0
        }

        reactor.start()
    }

    public override fun close() {
        editFunctionQueriesQueue.dispose()
        repliesQueue.dispose()
        serverFunctions.dispose()
        reactor.close()
        ctx.close()
    }

    init {
        initServer(this)
    }

    public override fun register(name: String, function: PayloadFunction, spec: FunctionSpec<*, *>) {
        editFunctionQueriesQueue.addFirst(RegisterFunctionQuery(name, function, spec))
    }

    public override fun unregister(name: String) {
        editFunctionQueriesQueue.addFirst(UnregisterFunctionQuery(name))
    }
}

internal expect fun initServer(server: ZmqTransportServer)

internal fun initServerBlocking(server: ZmqTransportServer) {
    server.frontend.bind("tcp://127.0.0.1:${server.port}")
    server.start()
}

internal sealed class EditFunctionQuery

private class RegisterFunctionQuery(val name: String, val function: PayloadFunction, val spec: FunctionSpec<*, *>) :
    EditFunctionQuery()

private class UnregisterFunctionQuery(val name: String) : EditFunctionQuery()

internal sealed class Response

internal class ResponseResult(val clientIdentity: ByteArray, val queryID: ByteArray, val resultBytes: ByteArray) :
    Response()

internal class ResponseException(val clientIdentity: ByteArray, val queryID: ByteArray, val exceptionMessage: String) :
    Response()

/**
 * Hack created because Kotlin/Native does not start event loop without runBlocking.
 *
 * Wraps [action] with `runBlocking` and executes if Kotlin/Native, calls [action] otherwise.
 */
internal expect inline fun runBlockingIfKotlinNative(crossinline action: () -> Any)

private fun ZmqTransportServer.handleReplyQueue() {
    while (true) {
        val reply = repliesQueue.removeLastOrNull() ?: break

        sendMsg(frontend) {
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
            is RegisterFunctionQuery -> {
                this@handleEditFunctionQueue.serverFunctions[editFunctionMessage.name] = editFunctionMessage.function
                this@handleEditFunctionQueue.serverFunctionSpecs[editFunctionMessage.name] = editFunctionMessage.spec
            }

            is UnregisterFunctionQuery -> this@handleEditFunctionQueue.serverFunctions -= editFunctionMessage.name
        }
    }
}
