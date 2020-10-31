package kscience.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.ZFrame

/** zframe_t object (CZMQ). */
internal actual class ZmqFrame(internal val handle: ZFrame) : Closeable, Iterable<Byte> {
    actual val data: ByteArray
        get() = handle.data

    actual override fun close(): Unit = handle.destroy()
    actual fun copy(): ZmqFrame = ZmqFrame(handle.duplicate())
    actual override fun iterator(): Iterator<Byte> = data.iterator()

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ZmqFrame) return false
        if (handle != other.handle) return false
        return true
    }

    actual override fun hashCode(): Int = handle.hashCode()
    actual override fun toString(): String = "ZmqFrame(data=${data.contentToString()})"

    actual companion object {
        actual fun recvFrame(socket: ZmqSocket): ZmqFrame = ZmqFrame(ZFrame.recvFrame(socket.handle))
    }
}
