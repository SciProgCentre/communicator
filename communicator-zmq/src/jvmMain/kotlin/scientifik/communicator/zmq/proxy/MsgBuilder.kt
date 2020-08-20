package scientifik.communicator.zmq.proxy

import scientifik.communicator.zmq.platform.ZmqFrame
import scientifik.communicator.zmq.platform.ZmqMsg
import scientifik.communicator.zmq.platform.ZmqSocket

internal class MsgBuilder(private val msg: ZmqMsg) {
    operator fun ByteArray.unaryPlus() {
        msg.add(this)
    }

    operator fun ZmqFrame.unaryPlus() {
        msg.add(this)
    }

    operator fun String.unaryPlus() {
        msg.add(this)
    }
}

internal inline fun sendMsg(socket: ZmqSocket, block: MsgBuilder.() -> Unit) {
    val msg = ZmqMsg()
    MsgBuilder(msg).block()
    msg.send(socket)
}
