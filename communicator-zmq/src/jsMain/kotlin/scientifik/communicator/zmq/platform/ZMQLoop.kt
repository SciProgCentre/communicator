package scientifik.communicator.zmq.platform

/** Constructor must create a loop with its "new" method */
actual class ZMQLoop actual constructor(ctx: ZMQContext) {
    actual fun addReader(socket: ZMQSocket, handler: (Any?, Any?, Any?) -> Int, arg: Any?) {
    }

    actual fun addTimer(delay: Int, times: Int, handler: (Any?, Any?, Any?) -> Int, arg: Any?) {
    }

    actual fun start() {
    }

}