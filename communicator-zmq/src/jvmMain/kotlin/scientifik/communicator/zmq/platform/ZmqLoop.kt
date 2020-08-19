package scientifik.communicator.zmq.platform

import org.zeromq.ZLoop
import org.zeromq.ZMQ

/** Constructor must create a loop with its "new" method */
internal actual class ZmqLoop actual constructor(ctx: ZmqContext) {

    internal val backendLoop = ZLoop(ctx.backendContext)

    actual fun addReader(socket: ZmqSocket, handler: (Any?, Any?, Any?) -> Int, arg: Any?) {
        val pollItem = ZMQ.PollItem(socket.backendSocket, ZMQ.Poller.POLLIN)
        backendLoop.addPoller(pollItem, handler, arg)
    }

    actual fun addTimer(delay: Int, times: Int, handler: (Any?, Any?, Any?) -> Int, arg: Any?) {
        backendLoop.addTimer(delay, times, handler, arg)
    }

    actual fun start() {
        backendLoop.start()
    }
}