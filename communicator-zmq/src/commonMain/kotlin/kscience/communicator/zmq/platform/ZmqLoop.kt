package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

internal expect class ZmqLoop(ctx: ZmqContext) : Closeable {
    inline fun <reified T : Any> addReader(
        socket: ZmqSocket,
        arg: Argument<T>,
        crossinline handler: ZmqLoop.(Argument<T>) -> Int,
    )

    inline fun <reified T : Any> addTimer(
        delay: Int,
        times: Int,
        arg: Argument<T>,
        crossinline handler: ZmqLoop.(Argument<T>) -> Int,
    )

    fun start()
    override fun close()

    class Argument<T : Any>(value: T) : Closeable {
        val value: T
        override fun close()
    }
}
