package space.kscience.communicator.zmq.platform

import io.ktor.utils.io.core.Closeable

internal expect class ZmqLoop(ctx: ZmqContext) {
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

    class Argument<out T : Any>(value: T) : Closeable {
        val value: T
        override fun close()
    }
}
