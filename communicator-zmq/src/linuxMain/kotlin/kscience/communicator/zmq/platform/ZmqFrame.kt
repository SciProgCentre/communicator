package kscience.communicator.zmq.platform

import czmq.*
import kotlinx.cinterop.*
import kotlinx.io.Closeable

/** zframe_t object (CZMQ). */
internal actual class ZmqFrame internal constructor(val backendFrame: CPointer<zframe_t>) : Closeable {
    actual val data: ByteArray
        get() = checkNotNull(zframe_data(backendFrame)).readBytes(zframe_size(backendFrame).toInt())

    override fun close(): Unit = memScoped {
        val cpv: CPointerVar<zframe_t> = alloc()
        cpv.value = backendFrame
        val a = allocPointerTo<CPointerVar<zframe_t>>()
        a.pointed = cpv
        zframe_destroy(a.value)
    }

    actual companion object {
        actual fun recvFrame(socket: ZmqSocket): ZmqFrame = ZmqFrame(checkNotNull(zframe_recv(socket.backendSocket)))
    }
}
