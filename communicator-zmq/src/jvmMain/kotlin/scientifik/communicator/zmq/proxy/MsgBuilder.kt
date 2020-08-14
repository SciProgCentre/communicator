package scientifik.communicator.zmq.proxy

import org.zeromq.ZFrame
import org.zeromq.ZMQ
import org.zeromq.ZMsg


class MsgBuilder(private val msg: ZMsg) {

    operator fun ByteArray.unaryPlus() {
        msg.add(this)
    }

    operator fun ZFrame.unaryPlus() {
        msg.add(this)
    }

    operator fun String.unaryPlus() {
        msg.add(this)
    }

}

inline fun sendMsg(socket: ZMQ.Socket, block: MsgBuilder.() -> Unit) {
    val msg = ZMsg()
    MsgBuilder(msg).block()
    msg.send(socket)
}