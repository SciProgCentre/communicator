package kscience.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.ZFrame

/** zframe_t object (CZMQ). */
internal actual class ZmqFrame(internal val handle: ZFrame) : Closeable {
    actual val data: ByteArray
        get() = handle.data

    actual override fun close(): Unit = handle.destroy()
    actual fun copy(): ZmqFrame = ZmqFrame(handle.duplicate())

    actual companion object {
        actual fun recvFrame(socket: ZmqSocket): ZmqFrame = ZmqFrame(ZFrame.recvFrame(socket.handle))
    }
}
