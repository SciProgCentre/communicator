package kscience.communicator.zmq.server

import io.ktor.utils.io.core.use
import kotlinx.coroutines.launch
import kscience.communicator.zmq.Protocol
import kscience.communicator.zmq.platform.ZmqFrame
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.util.sendMsg

internal fun ZmqTransportServer.handleFrontend() {
    var msg = ZmqMsg.recvMsg(frontend).use { it.map(ZmqFrame::data) }
    val (clientIdentity, type) = msg.let { (a, b) -> a to b.decodeToString() }
    println(msg.map(ByteArray::decodeToString))
    msg = msg.drop(2)

    when (type) {
        Protocol.Query -> {
            val (queryID, argBytes, functionName) = msg.let { Triple(it[0], it[1], it[2].decodeToString()) }

            frontend.sendMsg {
                +clientIdentity
                +Protocol.QueryReceived
                +queryID
            }

            val serverFunction = serverFunctions[functionName]?.first

            if (serverFunction == null)
                frontend.sendMsg {
                    +clientIdentity
                    +Protocol.Response.UnknownFunction
                    +queryID
                    +functionName
                }
            else
                workerScope.launch {
                    try {
                        val result = serverFunction(argBytes)
                        repliesQueue.addFirst(ResponseResult(clientIdentity, queryID, result))
                    } catch (ex: Exception) {
                        repliesQueue.addFirst(ResponseException(clientIdentity, queryID, ex.message.orEmpty()))
                    }
                }
        }

        Protocol.Coder.IdentityQuery -> {
            val (functionName) = msg
            val functionSpec = serverFunctions[functionName.decodeToString()]?.second

            frontend.sendMsg {
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
            val (_) = msg
            //TODO
        }

        else -> logger.warn { "Unknown message type: $type" }
    }
}
