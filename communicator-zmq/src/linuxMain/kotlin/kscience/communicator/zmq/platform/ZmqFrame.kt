package kscience.communicator.zmq.platform

import czmq.*
import kotlinx.cinterop.*
import kotlinx.io.Closeable

/** zframe_t object (CZMQ). */
internal actual class ZmqFrame internal constructor(val backendFrame: CPointer<zframe_t>) : Closeable {
    actual val data: ByteArray
        get() = checkNotNull(zframe_data(backendFrame)).readBytes(zframe_size(backendFrame).toInt())

    init {
        require(zframe_is(backendFrame)) { "Provided pointer $backendFrame doesn't point to zframe_t." }
    }

    override fun close(): Unit = memScoped {
        if (!zframe_is(backendFrame)) return@memScoped
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
