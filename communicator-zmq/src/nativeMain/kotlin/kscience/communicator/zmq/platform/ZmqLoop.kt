package kscience.communicator.zmq.platform

import kotlinx.cinterop.*
import kotlinx.io.Closeable
import org.zeromq.czmq.*
import kotlin.native.concurrent.AtomicInt

internal actual class ZmqLoop(val backendLoop: CPointer<zloop_t> = checkNotNull(zloop_new())) : Closeable {
    actual constructor(ctx: ZmqContext) : this()

    actual fun start() {
        zloop_start(backendLoop).checkZeroMQCode("zloop_start")
    }

    actual override fun close(): Unit = memScoped {
        val cpv: CPointerVar<zloop_t> = alloc()
        cpv.value = backendLoop
        val a = allocPointerTo<CPointerVar<zloop_t>>()
        a.pointed = cpv
        zloop_destroy(a.value)
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun <reified T : Any> addReader(
        socket: ZmqSocket,
        arg: Argument<T>,
        crossinline handler: ZmqLoop.(Argument<T>) -> Int,
    ) {
        zloop_reader(
            backendLoop,
            socket.backendSocket,

            staticCFunction { r, a, b ->
                val (argParam, handlerParam) = checkNotNull(
                    b?.asStableRef<Pair<T, ZmqLoop.(Any?, Argument<T>) -> Int>>()?.get()
                )

                ZmqLoop(checkNotNull(r)).handlerParam(
                    a?.let(::ZmqSocket),
                    Argument(argParam)
                )
            },

            arg.also {
                it.handler = { arg ->
                    if (T::class.isInstance(arg.value))
                        handler(arg)
                    else
                        error("Invalid argument received ${arg}.")
                }
            }.ref.asCPointer()
        )
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun <reified T : Any> addTimer(
        delay: Int,
        times: Int,
        arg: Argument<T>,
        crossinline handler: ZmqLoop.(Argument<T>) -> Int,
    ) {
        zloop_timer(
            backendLoop,
            delay.toULong(),
            times.toULong(),

            staticCFunction { r, a, b ->
                val (argParam, handlerParam) = checkNotNull(
                    b?.asStableRef<Pair<T, ZmqLoop.(Any?, Argument<T>) -> Int>>()?.get()
                )

                ZmqLoop(checkNotNull(r)).handlerParam(a, Argument(argParam))
            },

            arg.also {
                it.handler = { arg ->
                    if (T::class.isInstance(arg.value))
                        handler(arg)
                    else
                        error("Invalid argument received ${arg}.")
                }
            }.ref.asCPointer()
        )
    }

    actual class Argument<T : Any> actual constructor(actual val value: T) : Closeable {
        internal lateinit var handler: ZmqLoop.(Argument<T>) -> Int
        private val disposed: AtomicInt = AtomicInt(0)

        internal val ref: StableRef<Pair<T, ZmqLoop.(Argument<T>) -> Int>>
            get() = StableRef.create(value to handler)

        actual override fun close() {
            if (disposed.value == 0) {
                ref.dispose()
                disposed.value = 1
            }
        }
    }
}
