package scientifik.communicator.zmq.platform

/** zframe_t object (CZMQ). */
internal expect class ZmqFrame {

    val data: ByteArray

    fun close()

}