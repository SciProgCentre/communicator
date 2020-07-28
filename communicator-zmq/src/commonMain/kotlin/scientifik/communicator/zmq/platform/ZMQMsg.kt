package scientifik.communicator.zmq.platform

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
expect class ZMQMsg() {

    fun add(data: ByteArray)

    fun add(frame: ZMQFrame)

    fun pop(): ZMQFrame

    fun send(socket: ZMQSocket)

    fun close()

}