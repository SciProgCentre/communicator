package kscience.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.ZLoop
import org.zeromq.ZMQ

/** Constructor must create a loop with its "new" method */
internal actual class ZmqLoop private constructor(val backendLoop: ZLoop) : Closeable {
    actual constructor(ctx: ZmqContext) : this(ZLoop(ctx.backendContext))

    @Suppress("UNCHECKED_CAST")
    actual inline fun <reified T : Any> addReader(
        socket: ZmqSocket,
        arg: Argument<T>,
        crossinline handler: ZmqLoop.(Argument<T>) -> Int,
    ) {
        backendLoop.addPoller(
            ZMQ.PollItem(socket.backendSocket, ZMQ.Poller.POLLIN),
            { loop, _, argParam -> ZmqLoop(loop).handler(argParam as Argument<T>) },
            arg
        )
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun <reified T : Any> addTimer(
        delay: Int,
        times: Int,
        arg: Argument<T>,
        crossinline handler: ZmqLoop.(Argument<T>) -> Int,
    ) {
        backendLoop.addTimer(
            delay,
            times,
            { loop, _, argParam -> ZmqLoop(loop).handler(argParam as Argument<T>) },
            arg
        )
    }

    actual fun start() {
        backendLoop.start()
    }

    actual override fun close(): Unit = Unit

    actual class Argument<T : Any> actual constructor(actual val value: T) : Closeable {
        actual override fun close(): Unit = Unit
    }
}
