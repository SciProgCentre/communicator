package kscience.communicator.zmq.server

import co.touchlab.stately.collections.IsoArrayDeque
import co.touchlab.stately.collections.IsoMutableMap
import kotlinx.coroutines.*
import kotlinx.io.Closeable
import kotlinx.io.use
import kscience.communicator.api.PayloadFunction
import kscience.communicator.api.TransportServer
import kscience.communicator.zmq.platform.ZmqContext
import kscience.communicator.zmq.platform.ZmqLoop
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket

/**
 * Implements transport server with ZeroMQ-based machinery. Associated client transport is
 * [kscience.communicator.zmq.client.ZmqTransport].
 */
class ZmqTransportServer(override val port: Int) : TransportServer {
    internal class ServerState internal constructor(
        val port: Int,
        private val serverFunctions: IsoMutableMap<String, PayloadFunction> = IsoMutableMap(),
        private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
        private val workerScope: CoroutineScope = CoroutineScope(workerDispatcher + SupervisorJob()),
        private val ctx: ZmqContext = ZmqContext(),
        private val repliesQueue: IsoArrayDeque<Reply> = IsoArrayDeque(),
        internal val editFunctionQueriesQueue: IsoArrayDeque<EditFunctionQuery> = IsoArrayDeque(),
        internal val frontend: ZmqSocket = ctx.createDealerSocket()
    ) : Closeable {
        internal fun start() {
            val reactor = ZmqLoop(ctx)

            reactor.addReader(
                frontend,
                { _, arg ->
                    handleFrontend(arg?.value as FrontendHandlerArg)
                    0
                },
                ZmqLoop.Argument(FrontendHandlerArg(workerScope, frontend, serverFunctions, repliesQueue))
            )

            reactor.addTimer(
                1,
                0,
                { _, arg ->
                    handleReplyQueue(arg?.value as ReplyQueueHandlerArg)
                    0
                },
                ZmqLoop.Argument(ReplyQueueHandlerArg(frontend, repliesQueue))
            )

            reactor.addTimer(
                1,
                0,
                { _, arg ->
                    handleEditFunctionQueue(arg?.value as EditFunctionQueueHandlerArg)
                    0
                },
                ZmqLoop.Argument(EditFunctionQueueHandlerArg(serverFunctions, editFunctionQueriesQueue))
            )

            reactor.start()
        }

        override fun close() {
            editFunctionQueriesQueue.dispose()
            repliesQueue.dispose()
            serverFunctions.dispose()
            frontend.close()
            ctx.close()
        }
    }

    private val state = ServerState(port = port)

    init {
        initServer(state)
    }

    override fun register(name: String, function: PayloadFunction) {
        state.editFunctionQueriesQueue.addFirst(RegisterFunctionQuery(name, function))
    }

    override fun unregister(name: String) {
        state.editFunctionQueriesQueue.addFirst(UnregisterFunctionQuery(name))
    }

    override fun close(): Unit = state.close()
}

internal expect fun initServer(server: ZmqTransportServer.ServerState)

internal fun initServerBlocking(server: ZmqTransportServer.ServerState) {
    server.frontend.bind("tcp://127.0.0.1:${server.port}")
    server.start()
}

internal sealed class EditFunctionQuery
private class RegisterFunctionQuery(val name: String, val function: PayloadFunction) : EditFunctionQuery()
private class UnregisterFunctionQuery(val name: String) : EditFunctionQuery()

internal data class Reply(val queryID: ByteArray, val resultBytes: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Reply

        if (!queryID.contentEquals(other.queryID)) return false
        if (!resultBytes.contentEquals(other.resultBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = queryID.contentHashCode()
        result = 31 * result + resultBytes.contentHashCode()
        return result
    }
}

private class FrontendHandlerArg(
    val workerScope: CoroutineScope,
    val frontend: ZmqSocket,
    val serverFunctions: IsoMutableMap<String, PayloadFunction>,
    val repliesQueue: IsoArrayDeque<Reply>
)

/**
 * Hack created because Kotlin/Native does not start event loop without runBlocking.
 *
 * Wraps [action] with `runBlocking` and executes if Kotlin/Native, calls [action] otherwise.
 */
internal expect inline fun runBlockingIfKotlinNative(crossinline action: () -> Any)

private fun handleFrontend(arg: FrontendHandlerArg) = with(arg) {
    val msg = ZmqMsg.recvMsg(frontend)
    val queryID = msg.pop().data
    val functionName = msg.pop().data.decodeToString()
    val argBytes = msg.pop().data
    val serverFunction = checkNotNull(serverFunctions[functionName])
    runBlockingIfKotlinNative { workerScope.launch { repliesQueue.addFirst(Reply(queryID, serverFunction(argBytes))) } }
    Unit
}

private class ReplyQueueHandlerArg(
    val frontend: ZmqSocket,
    val repliesQueue: IsoArrayDeque<Reply>
)

private fun handleReplyQueue(arg: ReplyQueueHandlerArg): Unit = with(arg) {
    while (true) {
        val reply = repliesQueue.removeFirstOrNull() ?: break
        println("Received reply $reply from internal queue")

        ZmqMsg().use {
            it.add(reply.queryID)
            it.add(reply.resultBytes)
            it.send(frontend)
        }
    }
}

private class EditFunctionQueueHandlerArg(
    val serverFunctions: IsoMutableMap<String, PayloadFunction>,
    val editFunctionQueue: IsoArrayDeque<EditFunctionQuery>
)

private fun handleEditFunctionQueue(arg: EditFunctionQueueHandlerArg): Unit = with(arg) {
    while (true) {
        val editFunctionMessage = editFunctionQueue.removeFirstOrNull() ?: break

        when (editFunctionMessage) {
            is RegisterFunctionQuery -> arg.serverFunctions[editFunctionMessage.name] = editFunctionMessage.function
            is UnregisterFunctionQuery -> arg.serverFunctions.remove(editFunctionMessage.name)
        }
    }
}
