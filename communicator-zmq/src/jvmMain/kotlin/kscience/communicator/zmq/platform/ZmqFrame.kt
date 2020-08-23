package kscience.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.ZFrame

/** zframe_t object (CZMQ). */
internal actual class ZmqFrame(internal val backendFrame: ZFrame) : Closeable {
    actual val data: ByteArray
        get() = backendFrame.data

    override fun close(): Unit = backendFrame.destroy()

    actual companion object {
        actual fun recvFrame(socket: ZmqSocket): ZmqFrame = ZmqFrame(ZFrame.recvFrame(socket.backendSocket))
    }
}
