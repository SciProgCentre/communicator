package kscience.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.ZLoop
import org.zeromq.ZMQ

/** Constructor must create a loop with its "new" method */
internal actual class ZmqLoop private constructor(val backendLoop: ZLoop) : Closeable {
    actual constructor(ctx: ZmqContext) : this(ZLoop(ctx.backendContext))

    actual fun addReader(socket: ZmqSocket, handler: ZmqLoop.(Any?, Any?) -> Int, arg: Any?) {
        backendLoop.addPoller(
            ZMQ.PollItem(socket.backendSocket, ZMQ.Poller.POLLIN),
            { loop, item, argParam -> ZmqLoop(loop).handler(item, argParam) },
            arg
        )
    }

    actual fun addTimer(delay: Int, times: Int, handler: ZmqLoop.(Any?, Any?) -> Int, arg: Any?) {
        backendLoop.addTimer(delay, times, { loop, item, argParam -> ZmqLoop(loop).handler(item, argParam) }, arg)
    }

    actual fun start() {
        backendLoop.start()
    }

    override fun close(): Unit = Unit
}
