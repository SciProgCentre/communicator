package scientifik.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.ZMsg

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
internal actual class ZmqMsg(internal val backendMsg: ZMsg) : Closeable {

    actual constructor() : this(ZMsg())

    actual fun add(data: ByteArray) {
        backendMsg.add(data)
    }

    actual fun add(frame: ZmqFrame) {
        backendMsg.add(frame.backendFrame)
    }

    actual fun pop(): ZmqFrame = ZmqFrame(backendMsg.pop())

    actual fun send(socket: ZmqSocket) {
        backendMsg.send(socket.backendSocket)
    }

    actual override fun close(): Unit = backendMsg.destroy()

    fun add(s: String) {
        backendMsg.add(s)
    }
}