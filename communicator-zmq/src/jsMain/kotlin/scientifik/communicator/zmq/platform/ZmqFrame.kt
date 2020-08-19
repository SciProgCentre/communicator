package scientifik.communicator.zmq.platform

import kotlinx.io.Closeable

/** zframe_t object (CZMQ). */
internal actual class ZmqFrame : Closeable {
    actual val data: ByteArray
        get() = TODO("Not yet implemented")

    actual override fun close(): Unit = TODO()
}
