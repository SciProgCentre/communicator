package space.kscience.communicator.zmq.server

import io.ktor.utils.io.core.use
import kotlinx.coroutines.launch
import space.kscience.communicator.zmq.Protocol
import space.kscience.communicator.zmq.platform.ZmqFrame
import space.kscience.communicator.zmq.platform.ZmqMsg
import space.kscience.communicator.zmq.util.sendMsg

internal fun ZmqTransportServer.handleFrontend() {
    var msg = ZmqMsg.recvMsg(frontend).use { it.map(ZmqFrame::data) }
    val (clientIdentity, type) = msg.let { (a, b) -> a to b.decodeToString() }
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
                        logger.debug(ex) { "Exception from server function ($clientIdentity)" }
                        ex.printStackTrace() // TODO remove me
                        repliesQueue.addFirst(ResponseException(clientIdentity, queryID, ex.message.orEmpty()))
                    }
                }
        }

        Protocol.Coder.IdentityQuery -> {
            val (functionName) = msg

            val (_, argumentCodec, resultCodec) = serverFunctions[functionName.decodeToString()] ?: Triple(
                null,
                null,
                null,
            )

            frontend.sendMsg {
                +clientIdentity

                if (argumentCodec == null || resultCodec == null) {
                    +Protocol.Coder.IdentityNotFound
                    +functionName
                } else {
                    +Protocol.Coder.IdentityFound
                    +functionName
                    +argumentCodec.identity.encodeToByteArray()
                    +resultCodec.identity.encodeToByteArray()
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
