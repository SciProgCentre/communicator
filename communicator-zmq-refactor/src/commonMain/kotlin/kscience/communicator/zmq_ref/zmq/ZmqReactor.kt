package kscience.communicator.zmq_ref.zmq

// maybe loop is better
internal expect class ZmqReactor() {
    fun add(socket: ZmqSocket, handler: () -> Unit)
    fun poll()
    suspend fun suspendPoll()
}