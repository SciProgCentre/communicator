package scientifik.communicator.zmq.platform

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
expect class ZmqMsg() {

    fun add(data: ByteArray)

    fun add(frame: ZmqFrame)

    fun pop(): ZmqFrame

    fun send(socket: ZmqSocket)

    fun close()

}