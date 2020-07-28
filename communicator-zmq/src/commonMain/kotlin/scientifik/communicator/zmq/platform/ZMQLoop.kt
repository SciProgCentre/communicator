package scientifik.communicator.zmq.platform

/** Constructor must create a loop with its "new" method */
expect class ZMQLoop(ctx: ZMQContext) {

    fun addReader(socket: ZMQSocket, handler: (Any?, Any?, Any?) -> Int, arg: Any?)
    fun addTimer(delay: Int, times: Int, handler: (Any?, Any?, Any?) -> Int, arg: Any?)
    fun start()

}