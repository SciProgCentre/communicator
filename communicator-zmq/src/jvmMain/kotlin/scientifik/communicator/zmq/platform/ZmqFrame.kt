package scientifik.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.ZFrame

/** zframe_t object (CZMQ). */
internal actual class ZmqFrame(internal val backendFrame: ZFrame) : Closeable {
    actual val data: ByteArray
        get() = backendFrame.data

    actual override fun close(): Unit = backendFrame.destroy()
}
