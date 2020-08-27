package kscience.communicator.zmq.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kscience.communicator.api.FunctionSpec
import kscience.communicator.api.PayloadFunction
import kscience.communicator.zmq.platform.ZmqFrame
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

internal class FrontendHandlerArg(
    val workerScope: CoroutineScope,
    val frontend: ZmqSocket,
    val serverFunctions: MutableMap<String, PayloadFunction>,
    val serverFunctionSpecs: MutableMap<String, FunctionSpec<*, *>>,
    val repliesQueue: Channel<Response>
)

internal fun handleFrontend(arg: FrontendHandlerArg) = with(arg) {
    val msg = ZmqMsg.recvMsg(frontend)
    val msgBlocks = msg.map(ZmqFrame::data)
    val (clientIdentity, msgType) = msgBlocks
    val msgData = msgBlocks.drop(2)

    when (msgType.decodeToString()) {
        "QUERY" -> {
            val (queryID, argBytes, functionName) = msgData
            sendMsg(frontend) {
                +clientIdentity
                +"QUERY_RECEIVED"
                +queryID
            }
            val serverFunction = serverFunctions[functionName.decodeToString()]
            if (serverFunction == null) {
                sendMsg(frontend) {
                    +clientIdentity
                    +"RESPONSE_UNKNOWN_FUNCTION"
                    +queryID
                    +functionName
                }
            } else {
                workerScope.launch {
                    try {
                        val result = serverFunction(argBytes)
                        repliesQueue.send(ResponseResult(clientIdentity, queryID, result))
                    } catch (ex: Exception) {
                        repliesQueue.send(ResponseException(clientIdentity, queryID, ex.message.orEmpty()))
                    }
                }
            }
        }
        "CODER_IDENTITY_QUERY" -> {
            val (functionName) = msgData
            val functionSpec = serverFunctionSpecs[functionName.decodeToString()]
            sendMsg(frontend) {
                +clientIdentity
                if (functionSpec == null) {
                    +"CODER_IDENTITY_NOT_FOUND"
                    +functionName
                } else {
                    +"CODER_IDENTITY_FOUND"
                    +functionName
                    +functionSpec.argumentCoder.identity.encodeToByteArray()
                    +functionSpec.resultCoder.identity.encodeToByteArray()
                }
            }
        }
        "RESPONSE_RECEIVED" -> {
            val (queryID) = msgData
            //TODO
        }
        else -> {
            log.debug { "Unknown message type: ${msgType.decodeToString()}" }
        }
    }

    Unit
}
