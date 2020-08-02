package scientifik.communicator.zmq.platform

/** zframe_t object (CZMQ). */
expect class ZmqFrame {

    val data: ByteArray

    fun close()

}