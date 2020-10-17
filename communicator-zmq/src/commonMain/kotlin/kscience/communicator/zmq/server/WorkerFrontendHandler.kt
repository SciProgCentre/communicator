package kscience.communicator.zmq.server

import co.touchlab.stately.collections.IsoArrayDeque
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kscience.communicator.api.FunctionSpec
import kscience.communicator.api.PayloadFunction
import kscience.communicator.zmq.platform.ZmqFrame
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg

internal class WorkerFrontendHandlerArg(
    val workerScope: CoroutineScope,
    val frontend: ZmqSocket,
    val serverFunctions: MutableMap<String, PayloadFunction>,
    val serverFunctionSpecs: MutableMap<String, FunctionSpec<*, *>>,
    val repliesQueue: IsoArrayDeque<Response>
)

internal fun handleWorkerFrontend(arg: WorkerFrontendHandlerArg) = with(arg) {
    val msg = ZmqMsg.recvMsg(frontend)
    val msgBlocks = msg.map(ZmqFrame::data)
    val (msgType) = msgBlocks
    val msgData = msgBlocks.drop(1)

    when (msgType.decodeToString()) {
        "QUERY" -> {
            val (queryID, argBytes, functionName) = msgData

            sendMsg(frontend) {
                +"QUERY_RECEIVED"
                +queryID
            }

            val serverFunction = serverFunctions[functionName.decodeToString()]

            if (serverFunction == null) {
                sendMsg(frontend) {
                    +"RESPONSE_UNKNOWN_FUNCTION"
                    +queryID
                    +functionName
                }
            } else {
                workerScope.launch {
                    try {
                        val result = serverFunction(argBytes)
                        repliesQueue.addFirst(ResponseResult(byteArrayOf(), queryID, result))
                    } catch (ex: Exception) {
                        repliesQueue.addFirst(ResponseException(byteArrayOf(), queryID, ex.message.orEmpty()))
                    }
                }
            }
        }

        "RESPONSE_RECEIVED" -> {
            val (_) = msgData
            //TODO
        }

        "INCOMPATIBLE_SPECS_FAILURE" -> {
            val (functionName, argCoder, resultCoder) = msgData
            println("INCOMPATIBLE_SPECS_FAILURE functionName=$functionName argCoder=$argCoder resultCoder=$resultCoder")
        }

        else -> println("Unknown message type: ${msgType.decodeToString()}")
    }

    Unit
}
