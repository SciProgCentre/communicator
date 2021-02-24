package space.kscience.communicator.zmq.platform

import io.ktor.utils.io.core.Closeable
import org.zeromq.ZLoop
import org.zeromq.ZMQ

/** Constructor must create a loop with its "new" method */
internal actual class ZmqLoop private constructor(internal val handle: ZLoop) {
    actual constructor(ctx: ZmqContext) : this(ZLoop(ctx.handle))

    @Suppress("UNCHECKED_CAST")
    actual inline fun <reified T : Any> addReader(
        socket: ZmqSocket,
        arg: Argument<T>,
        crossinline handler: ZmqLoop.(Argument<T>) -> Int,
    ) {
        handle.addPoller(
            ZMQ.PollItem(socket.handle, ZMQ.Poller.POLLIN),
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
        handle.addTimer(
            delay,
            times,
            { loop, _, argParam -> ZmqLoop(loop).handler(argParam as Argument<T>) },
            arg
        )
    }

    actual fun start() {
        handle.start()
    }

    actual class Argument<T : Any> actual constructor(actual val value: T) : Closeable {
        actual override fun close(): Unit = Unit
    }
}
