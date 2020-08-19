package scientifik.communicator.zmq.platform

/** Constructor must create a loop with its "new" method */
internal actual class ZmqLoop actual constructor(ctx: ZmqContext) {
    actual fun addReader(socket: ZmqSocket, handler: (Any?, Any?, Any?) -> Int, arg: Any?): Unit = TODO()
    actual fun addTimer(delay: Int, times: Int, handler: (Any?, Any?, Any?) -> Int, arg: Any?): Unit = TODO()
    actual fun start(): Unit = TODO()
}
