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
        crossinline handler: ZmqLoop.(Any?, Argument<T>?) -> Int,
        arg: Argument<T>?
    ) {
        backendLoop.addPoller(
            ZMQ.PollItem(socket.backendSocket, ZMQ.Poller.POLLIN),
            { loop, item, argParam -> ZmqLoop(loop).handler(item, Argument(argParam as T)) },
            arg
        )
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun <reified T : Any> addTimer(
        delay: Int,
        times: Int,
        noinline handler: ZmqLoop.(Any?, Argument<T>?) -> Int,
        arg: Argument<T>?
    ) {
        backendLoop.addTimer(
            delay,
            times,
            { loop, item, argParam -> ZmqLoop(loop).handler(item, Argument(argParam as T)) },
            arg
        )
    }

    actual fun start() {
        backendLoop.start()
    }

    override fun close(): Unit = Unit

    actual class Argument<T : Any> actual constructor(actual val value: T) : Closeable {
        override fun close(): Unit = Unit
    }
}
