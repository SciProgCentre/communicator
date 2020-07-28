package scientifik.communicator.zmq.platform

import org.zeromq.ZFrame

/** zframe_t object (CZMQ). */
actual class ZMQFrame(val backupFrame: ZFrame) {

    actual val data: ByteArray
        get() = backupFrame.data

    actual fun close() {
        backupFrame.destroy()
    }

}