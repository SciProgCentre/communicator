package kscience.communicator.zmq.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kscience.communicator.api.FunctionSpec
import kscience.communicator.api.PayloadFunction
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

internal class WorkerFrontendHandlerArg(
    val workerScope: CoroutineScope,
    val frontend: ZmqSocket,
    val serverFunctions: MutableMap<String, PayloadFunction>,
    val serverFunctionSpecs: MutableMap<String, FunctionSpec<*, *>>,
    val repliesQueue: Channel<Response>
)

internal fun handleWorkerFrontend(arg: WorkerFrontendHandlerArg) = with(arg) {
    val msg = ZmqMsg.recvMsg(frontend)
    val msgBlocks = msg.map { it.data }
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
                        repliesQueue.send(ResponseResult(byteArrayOf(), queryID, result))
                    } catch (ex: Exception) {
                        repliesQueue.send(ResponseException(byteArrayOf(), queryID, ex.message.orEmpty()))
                    }
                }
            }
        }
        "RESPONSE_RECEIVED" -> {
            val (queryID) = msgData
            //TODO
        }
        "INCOMPATIBLE_SPECS_FAILURE" -> {
            val (functionName, argCoder, resultCoder) = msgData
            log.error { "INCOMPATIBLE_SPECS_FAILURE functionName=$functionName argCoder=$argCoder resultCoder=$resultCoder" }
        }
        else -> {
            log.debug { "Unknown message type: ${msgType.decodeToString()}" }
        }
    }

    Unit
}