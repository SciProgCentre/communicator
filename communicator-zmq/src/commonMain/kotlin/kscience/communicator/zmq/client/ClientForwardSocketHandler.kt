package kscience.communicator.zmq.client

import io.ktor.utils.io.core.use
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
    val clientContext: ZmqTransport,
)

internal fun handleForwardSocket(arg: ForwardSocketHandlerArg) = with(arg.clientContext) {
    logger.info { "Handling result ($identity)." }
    var msg = ZmqMsg.recvMsg(arg.socket).use { it.map(ZmqFrame::data) }
    val type = msg.first().decodeToString()
    msg = msg.drop(1)

    when (type) {
        Protocol.Response.Result -> {
            val (queryID, resultBytes) = msg

            arg.socket.sendMsg {
                +Protocol.Response.Received
                +queryID
            }

            val callback = queriesInWork[UniqueID(queryID)] ?: return
            callback.onResult(resultBytes)
        }

        Protocol.Response.Exception -> {
            val (queryID, exceptionMessage) = msg

            arg.socket.sendMsg {
                +Protocol.Response.Received
                +queryID
            }

            val callback = queriesInWork[UniqueID(queryID)] ?: return
            callback.onError(RemoteFunctionException(exceptionMessage.decodeToString()))
        }

        Protocol.Response.UnknownFunction -> {
            val (queryID, functionName) = msg

            arg.socket.sendMsg {
                +Protocol.Response.Received
                +queryID
            }

            val callback = queriesInWork[UniqueID(queryID)] ?: return
            callback.onError(UnsupportedFunctionNameException(functionName.decodeToString()))
        }

        Protocol.Coder.IdentityFound -> {
            val (queryID, argCoderIdentity, resultCoderIdentity) = msg
            val callback = specQueriesInWork[UniqueID(queryID)] ?: return
            callback.onSpecFound(argCoderIdentity.decodeToString(), resultCoderIdentity.decodeToString())
        }

        Protocol.Coder.IdentityNotFound -> {
            val (queryID) = msg
            val callback = specQueriesInWork[UniqueID(queryID)] ?: return
            callback.onSpecNotFound()
        }

        Protocol.QueryReceived -> {
            val (_) = msg
            //TODO
        }

        else -> logger.warn { "Unknown message type: $type" }
    }
}
