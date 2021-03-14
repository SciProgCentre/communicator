package space.kscience.communicator.zmq.server

import io.ktor.utils.io.core.use
import kotlinx.coroutines.launch
import space.kscience.communicator.zmq.Protocol
import space.kscience.communicator.zmq.platform.ZmqFrame
import space.kscience.communicator.zmq.platform.ZmqMsg
import space.kscience.communicator.zmq.util.sendMsg

internal fun ZmqWorker.handleWorkerFrontend() {
    var msg = ZmqMsg.recvMsg(frontend).use { it.map(ZmqFrame::data) }
    val type = msg.first().decodeToString()
    msg = msg.drop(1)

    when (type) {
        Protocol.Query -> {
            val (queryID, argBytes, functionName) = msg

            frontend.sendMsg {
                +Protocol.QueryReceived
                +queryID
            }

            val serverFunction = serverFunctions[functionName.decodeToString()]?.first

            if (serverFunction == null)
                frontend.sendMsg {
                    +Protocol.Response.UnknownFunction
                    +queryID
                    +functionName
                }
            else
                workerScope.launch {
                    try {
                        val result = serverFunction(argBytes)
                        repliesQueue.addFirst(ResponseResult(byteArrayOf(), queryID, result))
                    } catch (ex: Exception) {
                        repliesQueue.addFirst(ResponseException(byteArrayOf(), queryID, ex.message.orEmpty()))
                    }
                }
        }

        Protocol.Response.Received -> {
            val (_) = msg
            //TODO
        }

        Protocol.IncompatibleSpecsFailure -> {
            val (functionName, argCoder, resultCoder) = msg
            logger.warn { "INCOMPATIBLE_SPECS_FAILURE functionName=$functionName argCoder=$argCoder resultCoder=$resultCoder" }
        }

        else -> logger.warn { "Unknown message type: $type" }
    }
}
