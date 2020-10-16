package kscience.communicator.zmq.client

import kscience.communicator.api.RemoteFunctionException
import kscience.communicator.api.UnsupportedFunctionNameException
import kscience.communicator.zmq.platform.UniqueID
import kscience.communicator.zmq.platform.ZmqFrame
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg


internal class ForwardSocketHandlerArg(
    val socket: ZmqSocket,
    val clientContext: ClientState
)

internal fun ClientState.handleForwardSocket(arg: ForwardSocketHandlerArg) {
    log.debug { "Handling result" }
    val msg = ZmqMsg.recvMsg(arg.socket)
    val msgType = msg.pop().data
    val msgData = msg.map(ZmqFrame::data)
    when (msgType.decodeToString()) {
        "RESPONSE_RESULT" -> {
            val (queryID, resultBytes) = msgData
            sendMsg(arg.socket) {
                +"RESPONSE_RECEIVED"
                +queryID
            }
            val callback = queriesInWork[UniqueID(queryID)] ?: return
            callback.onResult(resultBytes)
        }
        "RESPONSE_EXCEPTION" -> {
            val (queryID, exceptionMessage) = msgData
            sendMsg(arg.socket) {
                +"RESPONSE_RECEIVED"
                +queryID
            }
            val callback = queriesInWork[UniqueID(queryID)] ?: return
            callback.onError(RemoteFunctionException(exceptionMessage.decodeToString()))
        }
        "RESPONSE_UNKNOWN_FUNCTION" -> {
            val (queryID, functionName) = msgData
            sendMsg(arg.socket) {
                +"RESPONSE_RECEIVED"
                +queryID
            }
            val callback = queriesInWork[UniqueID(queryID)] ?: return
            callback.onError(UnsupportedFunctionNameException(functionName.decodeToString()))
        }
        "CODER_IDENTITY_FOUND" -> {
            val (queryID, argCoderIdentity, resultCoderIdentity) = msgData
            val callback = specQueriesInWork[UniqueID(queryID)] ?: return
            callback.onSpecFound(argCoderIdentity.decodeToString(), resultCoderIdentity.decodeToString())
        }
        "CODER_IDENTITY_NOT_FOUND" -> {
            val (queryID) = msgData
            val callback = specQueriesInWork[UniqueID(queryID)] ?: return
            callback.onSpecNotFound()
        }
        "QUERY_RECEIVED" -> {
            val (queryID) = msgData
            //TODO
        }
        else -> {
            log.debug { "Unknown message type: ${msgType.decodeToString()}" }
        }
    }
}
