package scientifik.communicator.zmq.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import scientifik.communicator.api.PayloadFunction
import scientifik.communicator.api.TransportServer
import scientifik.communicator.api.log
import scientifik.communicator.zmq.platform.*

class ZMQTransportServer(override val port: Int) : TransportServer {

    private val serverFunctions = HashMap<String, PayloadFunction>()
    private val workerDispatcher = Dispatchers.Default
    private val workerScope = CoroutineScope(workerDispatcher + SupervisorJob())
    private val ctx = ZmqContext()
    private val repliesQueue = ConcurrentQueue<Reply>()
    private val editFunctionQueriesQueue = ConcurrentQueue<EditFunctionQuery>()

    init {
        //TODO
        runInBackground({}) {
            val frontend = ctx.createDealerSocket()
            frontend.bind("tcp://*:$port")
            start(frontend)
        }
    }

    override fun register(name: String, function: PayloadFunction) {
        editFunctionQueriesQueue.add(RegisterFunctionQuery(name, function))
    }

    override fun unregister(name: String) {
        editFunctionQueriesQueue.add(UnregisterFunctionQuery(name))
    }

    override fun stop() {
        //TODO
    }

    private fun start(frontend: ZmqSocket) {
        val reactor = ZmqLoop(ctx)
        reactor.addReader(frontend, { _, _, arg ->
            handleFrontend(arg as FrontendHandlerArg)
            0
        }, FrontendHandlerArg(workerScope, frontend, serverFunctions, repliesQueue))
        reactor.addTimer(1, 0, { _, _, arg ->
            handleReplyQueue(arg as ReplyQueueHandlerArg)
            0
        }, ReplyQueueHandlerArg(frontend, repliesQueue))
        reactor.addTimer(1, 0, { _, _, arg ->
            handleEditFunctionQueue(arg as EditFunctionQueueHandlerArg)
            0
        }, EditFunctionQueueHandlerArg(serverFunctions, editFunctionQueriesQueue))
        reactor.start()
    }

}

private sealed class EditFunctionQuery
private class RegisterFunctionQuery(val name: String, val function: PayloadFunction) : EditFunctionQuery()
private class UnregisterFunctionQuery(val name: String) : EditFunctionQuery()

private class Reply(val queryID: ByteArray, val resultBytes: ByteArray)

private class FrontendHandlerArg(
        val workerScope: CoroutineScope,
        val frontend: ZmqSocket,
        val serverFunctions: HashMap<String, PayloadFunction>,
        val repliesQueue: ConcurrentQueue<Reply>
)


private fun handleFrontend(arg: FrontendHandlerArg) {
    with(arg) {
        val msg: ZmqMsg = frontend.recvMsg()
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

private fun handleReplyQueue(arg: ReplyQueueHandlerArg) {
    with(arg) {
        while (true) {
            val reply = repliesQueue.poll() ?: break
            log("Received reply $reply from internal queue")
            val msg: ZmqMsg = ZmqMsg()
            msg.add(reply.queryID)
            msg.add(reply.resultBytes)
            msg.send(frontend)
        }
    }
}

private class EditFunctionQueueHandlerArg(
        val serverFunctions: HashMap<String, PayloadFunction>,
        val editFunctionQueue: ConcurrentQueue<EditFunctionQuery>
)

private fun handleEditFunctionQueue(arg: EditFunctionQueueHandlerArg) {
    with(arg) {
        while (true) {
            val editFunctionMessage = editFunctionQueue.poll() ?: break
            when (editFunctionMessage) {
                is RegisterFunctionQuery -> {
                    arg.serverFunctions[editFunctionMessage.name] = editFunctionMessage.function
                }
                is UnregisterFunctionQuery -> {
                    arg.serverFunctions.remove(editFunctionMessage.name)
                }
            }
        }
    }
}