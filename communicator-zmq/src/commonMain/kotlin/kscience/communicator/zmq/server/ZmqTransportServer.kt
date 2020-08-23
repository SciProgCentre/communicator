package kscience.communicator.zmq.server

import co.touchlab.stately.collections.IsoArrayDeque
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.io.Closeable
import kotlinx.io.use
import kscience.communicator.api.PayloadFunction
import kscience.communicator.api.TransportServer
import kscience.communicator.zmq.platform.ZmqContext
import kscience.communicator.zmq.platform.ZmqLoop
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket

//import mu.KLogger
//import mu.KotlinLogging

/**
 * Implements transport server with ZeroMQ-based machinery. Associated client transport is
 * [kscience.communicator.zmq.client.ZmqTransport].
 */
class ZmqTransportServer(override val port: Int) : TransportServer {
    //    internal val log: KLogger = KotlinLogging.logger("ZmqTransportServer")
    private val editFunctionQueriesQueue: IsoArrayDeque<EditFunctionQuery> = IsoArrayDeque()

    internal class ServerState internal constructor(
        val port: Int,
        private val serverFunctions: MutableMap<String, PayloadFunction> = hashMapOf(),
        private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
        private val workerScope: CoroutineScope = CoroutineScope(workerDispatcher + SupervisorJob()),
        private val ctx: ZmqContext = ZmqContext(),
        private val repliesQueue: Channel<Reply> = Channel(),
        private val editFunctionQueriesQueue: IsoArrayDeque<EditFunctionQuery>,
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
            repliesQueue.close()
            frontend.close()
            ctx.close()
        }
    }

    private val state = ServerState(port = port, editFunctionQueriesQueue = editFunctionQueriesQueue)

    init {
        initServer(
            state
        )
    }

    override fun register(name: String, function: PayloadFunction) {
        editFunctionQueriesQueue.addFirst(RegisterFunctionQuery(name, function))
    }

    override fun unregister(name: String) {
        editFunctionQueriesQueue.addFirst(UnregisterFunctionQuery(name))
    }

    override fun close() {
        state.close()
        editFunctionQueriesQueue.dispose()
    }
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
    val serverFunctions: MutableMap<String, PayloadFunction>,
    val repliesQueue: SendChannel<Reply>
)

private fun handleFrontend(arg: FrontendHandlerArg) = with(arg) {
    val msg = ZmqMsg.recvMsg(frontend)
    val queryID = msg.pop().data
    val functionName = msg.pop().data.decodeToString()
    val argBytes = msg.pop().data
    val serverFunction = checkNotNull(serverFunctions[functionName])

    workerScope.launch {
        val result = serverFunction(argBytes)
        repliesQueue.send(Reply(queryID, result))
    }

    Unit
}

private class ReplyQueueHandlerArg(
    val frontend: ZmqSocket,
    val repliesQueue: ReceiveChannel<Reply>
)

private fun /*ZmqTransportServer.*/handleReplyQueue(arg: ReplyQueueHandlerArg): Unit = with(arg) {
    while (true) {
        val reply = repliesQueue.poll() ?: break
//        log.info { "Received reply $reply from internal queue" }

        ZmqMsg().use {
            it.add(reply.queryID)
            it.add(reply.resultBytes)
            it.send(frontend)
        }
    }
}

private class EditFunctionQueueHandlerArg(
    val serverFunctions: MutableMap<String, PayloadFunction>,
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
