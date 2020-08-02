package scientifik.communicator.zmq.platform

/** Constructor must create a loop with its "new" method */
internal expect class ZmqLoop(ctx: ZmqContext) {

    fun addReader(socket: ZmqSocket, handler: (Any?, Any?, Any?) -> Int, arg: Any?)
    fun addTimer(delay: Int, times: Int, handler: (Any?, Any?, Any?) -> Int, arg: Any?)
    fun start()

    fun destroy()

}