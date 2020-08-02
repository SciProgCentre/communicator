package scientifik.communicator.zmq.platform

import org.zeromq.ZFrame

/** zframe_t object (CZMQ). */
internal actual class ZmqFrame(internal val backendFrame: ZFrame) {

    actual val data: ByteArray
        get() = backendFrame.data

    actual fun close() {
        backendFrame.destroy()
    }

}