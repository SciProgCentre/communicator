package kscience.communicator.zmq.server

import kotlinx.coroutines.launch
import kscience.communicator.zmq.Protocol
import kscience.communicator.zmq.platform.ZmqFrame
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.util.sendMsg

internal fun ZmqTransportServer.handleFrontend() {
    val msg = ZmqMsg.recvMsg(frontend)
    val msgBlocks = msg.map(ZmqFrame::data)
    val (clientIdentity, msgType) = msgBlocks
    val msgData = msgBlocks.drop(2)

    when (msgType.decodeToString()) {
        Protocol.Query -> {
            val (queryID, argBytes, functionName) = msgData

            frontend.sendMsg() {
                +clientIdentity
                +Protocol.QueryReceived
                +queryID
            }

            val serverFunction = serverFunctions[functionName.decodeToString()]

            if (serverFunction == null)
                frontend.sendMsg() {
                    +clientIdentity
                    +Protocol.Response.UnknownFunction
                    +queryID
                    +functionName
                }
            else
                runBlockingIfKotlinNative {
                    workerScope.launch {
                        try {
                            val result = serverFunction(argBytes)
                            repliesQueue.addFirst(ResponseResult(clientIdentity, queryID, result))
                        } catch (ex: Exception) {
                            repliesQueue.addFirst(ResponseException(clientIdentity, queryID, ex.message.orEmpty()))
                        }
                    }
                }
        }

        Protocol.Coder.IdentityQuery -> {
            val (functionName) = msgData
            val functionSpec = serverFunctionSpecs[functionName.decodeToString()]

            frontend.sendMsg() {
                +clientIdentity

                if (functionSpec == null) {
                    +Protocol.Coder.IdentityNotFound
                    +functionName
                } else {
                    +Protocol.Coder.IdentityFound
                    +functionName
                    +functionSpec.argumentCoder.identity.encodeToByteArray()
                    +functionSpec.resultCoder.identity.encodeToByteArray()
                }
            }
        }

        Protocol.Response.Received -> {
            val (_) = msgData
            //TODO
        }

        else -> logger.warn { "Unknown message type: ${msgType.decodeToString()}" }
    }
}
