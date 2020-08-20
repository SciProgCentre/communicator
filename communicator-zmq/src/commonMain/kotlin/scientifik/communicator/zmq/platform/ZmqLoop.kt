package scientifik.communicator.zmq.platform

import kotlinx.io.Closeable

/** Constructor must create a loop with its "new" method */
internal expect class ZmqLoop(ctx: ZmqContext) : Closeable {
    fun addReader(socket: ZmqSocket, handler: (Any?, Any?, Any?) -> Int, arg: Any?)
    fun addTimer(delay: Int, times: Int, handler: (Any?, Any?, Any?) -> Int, arg: Any?)
    fun start()
}
