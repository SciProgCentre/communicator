package kscience.communicator.zmq.platform

import kotlinx.cinterop.*
import kotlinx.io.Closeable
import org.zeromq.czmq.*

/** zframe_t object (CZMQ). */
internal actual class ZmqFrame internal constructor(val handle: CPointer<zframe_t>) : Closeable, Iterable<Byte> {
    actual val data: ByteArray
        get() = checkNotNull(zframe_data(handle)) { "zframe_data returned null." }
            .readBytes(zframe_size(handle).toInt())

    init {
        require(zframe_is(handle)) { "Provided pointer $handle doesn't point to zframe_t." }
    }

    actual override fun close(): Unit = memScoped {
        if (!zframe_is(handle)) return@memScoped
        val cpv = alloc<CPointerVar<zframe_t>>()
        cpv.value = handle
        val a = allocPointerTo<CPointerVar<zframe_t>>()
        a.pointed = cpv
        zframe_destroy(a.value)
    }

    actual fun copy(): ZmqFrame = ZmqFrame(checkNotNull(zframe_dup(handle)) { "zframe_dup returned null." })

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ZmqFrame) return false
        return zframe_eq(handle, other.handle)
    }

    actual override fun hashCode(): Int = data.contentHashCode()
    actual override fun iterator(): Iterator<Byte> = data.iterator()
    actual override fun toString(): String = "ZmqFrame(data=${data.contentToString()})"

    actual companion object {
        actual fun recvFrame(socket: ZmqSocket): ZmqFrame =
            ZmqFrame(checkNotNull(zframe_recv(socket.handle)) { "zframe_recv was interrupted." })
    }
}
