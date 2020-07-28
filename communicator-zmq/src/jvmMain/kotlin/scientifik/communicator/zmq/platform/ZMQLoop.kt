package scientifik.communicator.zmq.platform

import org.zeromq.ZLoop
import org.zeromq.ZMQ

/** Constructor must create a loop with its "new" method */
actual class ZMQLoop actual constructor(ctx: ZMQContext) {

    val backupLoop = ZLoop(ctx.backupContext)

    actual fun addReader(socket: ZMQSocket, handler: (Any?, Any?, Any?) -> Int, arg: Any?) {
        val pollItem = ZMQ.PollItem(socket.backupSocket, ZMQ.Poller.POLLIN)
        backupLoop.addPoller(pollItem, handler, arg)
    }

    actual fun addTimer(delay: Int, times: Int, handler: (Any?, Any?, Any?) -> Int, arg: Any?) {
        backupLoop.addTimer(delay, times, handler, arg)
    }

    actual fun start() {
        backupLoop.start()
    }

}