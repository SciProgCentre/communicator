package scientifik.communicator.zmq.server

import kotlinx.coroutines.*
import kotlinx.io.use
import mu.KLogger
import mu.KotlinLogging
import scientifik.communicator.api.PayloadFunction
import scientifik.communicator.api.TransportServer
import scientifik.communicator.zmq.platform.*

class ZmqTransportServer(override val port: Int) : TransportServer {
    internal val log: KLogger = KotlinLogging.logger("ZmqTransportServer")
    private val serverFunctions: MutableMap<String, PayloadFunction> = hashMapOf()
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default
    private val workerScope: CoroutineScope = CoroutineScope(workerDispatcher + SupervisorJob())
    private val ctx = ZmqContext()
    private val repliesQueue = ConcurrentQueue<Reply>()
    private val editFunctionQueriesQueue = ConcurrentQueue<EditFunctionQuery>()
    private val frontend: ZmqSocket = ctx.createDealerSocket()

    init {
        //TODO
        runInBackground({}) {
            frontend.bind("tcp://*:$port")
            start()
        }
    }

    override fun register(name: String, function: PayloadFunction) {
        editFunctionQueriesQueue.add(RegisterFunctionQuery(name, function))
    }

    override fun unregister(name: String) {
        editFunctionQueriesQueue.add(UnregisterFunctionQuery(name))
    }

    override fun close() {
        frontend.close()
        ctx.close()
    }

    private fun start() {
        val reactor = ZmqLoop(ctx)

        reactor.addReader(
            frontend,
            { _, _, arg ->
                handleFrontend(arg as FrontendHandlerArg)
                0
            },
            FrontendHandlerArg(workerScope, frontend, serverFunctions, repliesQueue)
        )

        reactor.addTimer(
            1,
            0,
            { _, _, arg ->
                handleReplyQueue(arg as ReplyQueueHandlerArg)
                0
            },
            ReplyQueueHandlerArg(frontend, repliesQueue)
        )

        reactor.addTimer(
            1,
            0,
            { _, _, arg ->
                handleEditFunctionQueue(arg as EditFunctionQueueHandlerArg)
                0
            },
            EditFunctionQueueHandlerArg(serverFunctions, editFunctionQueriesQueue)
        )

        reactor.start()
    }
}

private sealed class EditFunctionQuery
private class RegisterFunctionQuery(val name: String, val function: PayloadFunction) : EditFunctionQuery()
private class UnregisterFunctionQuery(val name: String) : EditFunctionQuery()

private data class Reply(val queryID: ByteArray, val resultBytes: ByteArray) {
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
    val repliesQueue: ConcurrentQueue<Reply>
)

private fun handleFrontend(arg: FrontendHandlerArg) {
    with(arg) {
        val msg = frontend.recvMsg()
        val queryID = msg.pop().data
        val functionName = msg.pop().data.decodeToString()
        val argBytes = msg.pop().data
        val serverFunction = serverFunctions[functionName]!!

        workerScope.launch {
            val result = serverFunction(argBytes)
            repliesQueue.add(Reply(queryID, result))
        }
    }
}

private class ReplyQueueHandlerArg(
    val frontend: ZmqSocket,
    val repliesQueue: ConcurrentQueue<Reply>
)

private fun ZmqTransportServer.handleReplyQueue(arg: ReplyQueueHandlerArg): Unit = with(arg) {
    while (true) {
        val reply = repliesQueue.poll() ?: break
        log.info { "Received reply $reply from internal queue" }

        ZmqMsg().use {
            it.add(reply.queryID)
            it.add(reply.resultBytes)
            it.send(frontend)
        }
    }
}

private class EditFunctionQueueHandlerArg(
    val serverFunctions: MutableMap<String, PayloadFunction>,
    val editFunctionQueue: ConcurrentQueue<EditFunctionQuery>
)

private fun handleEditFunctionQueue(arg: EditFunctionQueueHandlerArg): Unit = with(arg) {
    while (true) {
        val editFunctionMessage = editFunctionQueue.poll() ?: break

        when (editFunctionMessage) {
            is RegisterFunctionQuery -> arg.serverFunctions[editFunctionMessage.name] = editFunctionMessage.function
            is UnregisterFunctionQuery -> arg.serverFunctions.remove(editFunctionMessage.name)
        }
    }
}
