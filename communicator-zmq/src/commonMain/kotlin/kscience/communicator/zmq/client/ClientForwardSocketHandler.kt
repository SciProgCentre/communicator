package kscience.communicator.zmq.client

import kscience.communicator.api.RemoteFunctionException
import kscience.communicator.api.UnsupportedFunctionNameException
import kscience.communicator.zmq.Protocol
import kscience.communicator.zmq.platform.UniqueID
import kscience.communicator.zmq.platform.ZmqFrame
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg

internal class ForwardSocketHandlerArg(
    val socket: ZmqSocket,
    val clientContext: ZmqTransport
)

internal fun handleForwardSocket(arg: ForwardSocketHandlerArg) = with(arg.clientContext) {
    println("Handling result")
    val msg = ZmqMsg.recvMsg(arg.socket)
    msg.pop()
    val msgType = msg.pop().data
    val msgData = msg.map(ZmqFrame::data)

    when (msgType.decodeToString()) {
        Protocol.Response.Result -> {
            val (queryID, resultBytes) = msgData

            sendMsg(arg.socket) {
                +identity
                +Protocol.Response.Received
                +queryID
            }

            val callback = queriesInWork[UniqueID(queryID)] ?: return
            callback.onResult(resultBytes)
        }

        Protocol.Response.Exception -> {
            val (queryID, exceptionMessage) = msgData

            sendMsg(arg.socket) {
                +identity
                +Protocol.Response.Received
                +queryID
            }

            val callback = queriesInWork[UniqueID(queryID)] ?: return
            callback.onError(RemoteFunctionException(exceptionMessage.decodeToString()))
        }

        Protocol.Response.UnknownFunction -> {
            val (queryID, functionName) = msgData

            sendMsg(arg.socket) {
                +identity
                +Protocol.Response.Received
                +queryID
            }

            val callback = queriesInWork[UniqueID(queryID)] ?: return
            callback.onError(UnsupportedFunctionNameException(functionName.decodeToString()))
        }

        Protocol.Coder.IdentityFound -> {
            val (queryID, argCoderIdentity, resultCoderIdentity) = msgData
            val callback = specQueriesInWork[UniqueID(queryID)] ?: return
            callback.onSpecFound(argCoderIdentity.decodeToString(), resultCoderIdentity.decodeToString())
        }

        Protocol.Coder.IdentityNotFound -> {
            val (queryID) = msgData
            val callback = specQueriesInWork[UniqueID(queryID)] ?: return
            callback.onSpecNotFound()
        }

        Protocol.QueryReceived -> {
            val (_) = msgData
            //TODO
        }

        else -> println("Unknown message type: ${msgType.decodeToString()}")
    }
}
