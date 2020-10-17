package kscience.communicator.zmq.server

import kotlinx.coroutines.launch
import kscience.communicator.zmq.Protocol
import kscience.communicator.zmq.platform.ZmqFrame
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.util.sendMsg

internal fun handleWorkerFrontend(arg: ZmqWorker): Unit = with(arg) {
    val msg = ZmqMsg.recvMsg(frontend)
    val msgBlocks = msg.map(ZmqFrame::data)
    val (msgType) = msgBlocks
    val msgData = msgBlocks.drop(1)

    when (msgType.decodeToString()) {
        Protocol.Query -> {
            val (queryID, argBytes, functionName) = msgData

            sendMsg(frontend) {
                +Protocol.QueryReceived
                +queryID
            }

            val serverFunction = serverFunctions[functionName.decodeToString()]

            if (serverFunction == null)
                sendMsg(frontend) {
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
            val (_) = msgData
            //TODO
        }

        Protocol.IncompatibleSpecsFailure -> {
            val (functionName, argCoder, resultCoder) = msgData
            println("INCOMPATIBLE_SPECS_FAILURE functionName=$functionName argCoder=$argCoder resultCoder=$resultCoder")
        }

        else -> println("Unknown message type: ${msgType.decodeToString()}")
    }
}
