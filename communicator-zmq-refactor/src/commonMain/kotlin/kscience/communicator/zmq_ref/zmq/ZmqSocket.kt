package kscience.communicator.zmq_ref.zmq

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.io.Closeable


internal expect class ZmqSocket : Closeable {
    fun connect(zmqAddress: String)
    fun bind(zmqAddress: String)
    fun setIdentity(identity: ByteArray)
    fun recv(): ZmqMessage
    suspend fun suspendRecv(): ZmqMessage
    fun send(message: ZmqMessage, block: Boolean = true)
    override fun close()
}

internal fun ZmqSocket.send(message: String, block: Boolean = true) {
    val msg = ZmqMessage()
    msg.add(message)
    send(msg, block)
}

internal suspend fun ZmqSocket.suspendSend(message: ZmqMessage) {
    val res = GlobalScope.async {
        send(message)
    }

    return res.await()
}
