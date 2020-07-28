package scientifik.communicator.zmq.platform

expect class ZMQSocket {

    fun connect(zmqAddress: String)
    fun bind(zmqAddress: String)

    fun setIdentity(identity: ByteArray)

    /** zmsg_recv method (CZMQ) */
    fun recvMsg(): ZMQMsg

    fun close()

}