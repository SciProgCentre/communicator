package kscience.communicator.zmq.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kscience.communicator.api.PayloadFunction
import kscience.communicator.zmq.platform.ZmqSocket


internal class FrontendHandlerArg(
    val workerScope: CoroutineScope,
    val frontend: ZmqSocket,
    val serverFunctions: MutableMap<String, PayloadFunction>,
    val repliesQueue: Channel<Reply>
)

internal fun handleFrontend(arg: FrontendHandlerArg) {
    with(arg) {
        val msg = frontend.recvMsg()
        val queryID = msg.pop().data
        val functionName = msg.pop().data.decodeToString()
        val argBytes = msg.pop().data
        val serverFunction = serverFunctions[functionName]!!

        workerScope.launch {
            val result = serverFunction(argBytes)
            repliesQueue.send(Reply(queryID, result))
        }
    }
}