package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

internal expect class ZmqLoop(ctx: ZmqContext) : Closeable {
    inline fun <reified T : Any> addReader(
        socket: ZmqSocket,
        crossinline handler: ZmqLoop.(Any?, Argument<T>) -> Int,
        arg: Argument<T>
    )

    inline fun <reified T : Any> addTimer(
        delay: Int,
        times: Int,
        noinline handler: ZmqLoop.(Any?, Argument<T>) -> Int,
        arg: Argument<T>
    )

    fun start()
    override fun close()

    class Argument<T : Any>(value: T) : Closeable {
        val value: T
        override fun close()
    }
}
