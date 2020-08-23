package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

/** Constructor must create a loop with its "new" method */
internal actual class ZmqLoop actual constructor(ctx: ZmqContext) : Closeable {
    actual fun start(): Unit = TODO()
    override fun close(): Unit = TODO()

    actual inline fun <reified T : Any> addReader(
        socket: ZmqSocket,
        crossinline handler: ZmqLoop.(Any?, Argument<T>?) -> Int,
        arg: Argument<T>?
    ): Unit = TODO()

    actual inline fun <reified T : Any> addTimer(
        delay: Int,
        times: Int,
        noinline handler: ZmqLoop.(Any?, Argument<T>?) -> Int,
        arg: Argument<T>?
    ): Unit = TODO()

    actual class Argument<T : Any> actual constructor(value: T) : Closeable {
        actual val value: T
            get() = TODO()

        override fun close() = TODO()
    }
}
