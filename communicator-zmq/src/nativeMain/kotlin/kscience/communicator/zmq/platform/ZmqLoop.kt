package kscience.communicator.zmq.platform

import kotlinx.cinterop.*
import kotlinx.io.Closeable
import org.zeromq.czmq.*
import kotlin.native.concurrent.AtomicInt

private typealias ArgumentData<T> = Pair<T, ZmqLoop.(ZmqLoop.Argument<T>) -> Int>

private inline fun <reified T : Any> loopCallbackHandler(
    arg: COpaquePointer?,
    loop: CPointer<zloop_t>?
): Int {
    val (value, handler) = checkNotNull(arg?.asStableRef<ArgumentData<T>>()?.get()) {
        "zloop callback argument is null or cannot be read."
    }

    return ZmqLoop(checkNotNull(loop) { "zloop callback zloop_t pointer is null." }).handler(ZmqLoop.Argument(value))
}

internal actual class ZmqLoop(
    internal val handle: CPointer<zloop_t> = checkNotNull(zloop_new()) { "zloop_new returned null." }
) {
    actual constructor(ctx: ZmqContext) : this()

    actual fun start() {
        zloop_start(handle)
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun <reified T : Any> addReader(
        socket: ZmqSocket,
        arg: Argument<T>,
        crossinline handler: ZmqLoop.(Argument<T>) -> Int,
    ): Unit = zloop_reader1(
        handle,
        socket.handle,
        staticCFunction { loop, _, argPtr -> loopCallbackHandler<T>(argPtr, loop) },

        arg.also {
            it.handler = { arg ->
                if (T::class.isInstance(arg.value))
                    handler(arg)
                else
                    error("Invalid callback argument is received from zloop: ${arg}.")
            }
        }.ref.asCPointer()
    ).checkReturnState("zloop_timer")

    @Suppress("UNCHECKED_CAST")
    actual inline fun <reified T : Any> addTimer(
        delay: Int,
        times: Int,
        arg: Argument<T>,
        crossinline handler: ZmqLoop.(Argument<T>) -> Int,
    ): Unit = zloop_timer1(
        handle,
        delay.toULong(),
        times.toULong(),
        staticCFunction { loop, _, argPtr -> loopCallbackHandler<T>(argPtr, loop) },

        arg.also {
            it.handler = { arg ->
                if (T::class.isInstance(arg.value))
                    handler(arg)
                else
                    error("Invalid callback argument is received from zloop: ${arg}.")
            }
        }.ref.asCPointer()
    ).checkReturnState("zloop_timer")

    actual class Argument<T : Any> actual constructor(actual val value: T) : Closeable {
        internal lateinit var handler: ZmqLoop.(Argument<T>) -> Int
        private val isDisposed: AtomicInt = AtomicInt(0)

        internal val ref: StableRef<ArgumentData<T>>
            get() = StableRef.create(value to handler)

        actual override fun close() {
            if (isDisposed.value != 0) return
            ref.dispose()
            isDisposed.value = 1
        }
    }
}
