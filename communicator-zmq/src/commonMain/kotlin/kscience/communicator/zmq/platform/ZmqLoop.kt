package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

/** Constructor must create a loop with its "new" method */
internal expect class ZmqLoop(ctx: ZmqContext) : Closeable {
    inline fun <reified T : Any> addReader(
        socket: ZmqSocket,
        crossinline handler: ZmqLoop.(Any?, Argument<T>?) -> Int,
        arg: Argument<T>?
    )

    inline fun <reified T : Any> addTimer(
        delay: Int,
        times: Int,
        noinline handler: ZmqLoop.(Any?, Argument<T>?) -> Int,
        arg: Argument<T>?
    )

    fun start()

    class Argument<T : Any>(value: T) : Closeable {
        val value: T
    }
}
