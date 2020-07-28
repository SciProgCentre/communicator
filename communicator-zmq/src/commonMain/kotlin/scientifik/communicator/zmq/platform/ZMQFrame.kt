package scientifik.communicator.zmq.platform

/** zframe_t object (CZMQ). */
expect class ZMQFrame {

    val data: ByteArray

    fun close()

}