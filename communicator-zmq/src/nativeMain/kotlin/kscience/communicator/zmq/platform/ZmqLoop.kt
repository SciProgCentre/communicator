package kscience.communicator.zmq.platform

import org.zeromq.czmq.*
import kotlinx.cinterop.*
import kotlinx.io.Closeable
import kotlin.native.concurrent.AtomicInt

/** Constructor must create a loop with its "new" method */
internal actual class ZmqLoop(val backendLoop: CPointer<zloop_t> = checkNotNull(zloop_new())) : Closeable {
    actual constructor(ctx: ZmqContext) : this()

    actual fun start() {
        zloop_start(backendLoop).checkZeroMQCode("zloop_start")
    }

    override fun close(): Unit = memScoped {
        val cpv: CPointerVar<zloop_t> = alloc()
        cpv.value = backendLoop
        val a = allocPointerTo<CPointerVar<zloop_t>>()
        a.pointed = cpv
        zloop_destroy(a.value)
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun <reified T : Any> addReader(
        socket: ZmqSocket,
        crossinline handler: ZmqLoop.(Any?, Argument<T>?) -> Int,
        arg: Argument<T>?
    ) {
        zloop_reader(
            backendLoop,
            socket.backendSocket,

            staticCFunction { r, a, b ->
                val (argParam, handlerParam) = checkNotNull(
                    b?.asStableRef<Pair<T, ZmqLoop.(Any?, Argument<T>?) -> Int>>()?.get()
                )

                ZmqLoop(checkNotNull(r)).handlerParam(
                    a?.let(::ZmqSocket),
                    Argument(argParam)
                )
            },

            (arg ?: Argument(Unit)).also {
                it.handler = { any, arg ->
                    if (T::class.isInstance(arg?.value))
                        handler(any, arg as Argument<T>?)
                    else
                        handler(any, null)
                }
            }.ref.asCPointer()
        )
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun <reified T : Any> addTimer(
        delay: Int,
        times: Int,
        noinline handler: ZmqLoop.(Any?, Argument<T>?) -> Int,
        arg: Argument<T>?
    ) {
        zloop_timer(
            backendLoop,
            delay.toULong(),
            times.toULong(),

            staticCFunction { r, a, b ->
                val (argParam, handlerParam) = checkNotNull(
                    b?.asStableRef<Pair<T, ZmqLoop.(Any?, Argument<T>?) -> Int>>()?.get()
                )

                ZmqLoop(checkNotNull(r)).handlerParam(
                    a,
                    Argument(argParam)
                )
            },

            (arg ?: Argument(Unit)).also {
                it.handler = { any, arg ->
                    if (T::class.isInstance(arg?.value))
                        handler(any, arg as Argument<T>?)
                    else
                        handler(any, null)
                }
            }.ref.asCPointer()
        )
    }

    actual class Argument<T : Any> actual constructor(actual val value: T) : Closeable {
        internal lateinit var handler: ZmqLoop.(Any?, Argument<T>?) -> Int
        private val disposed: AtomicInt = AtomicInt(0)

        internal val ref: StableRef<Pair<T, ZmqLoop.(Any?, Argument<T>?) -> Int>>
            get() = StableRef.create(value to handler)

        override fun close() {
            if (disposed.value == 0) {
                ref.dispose()
                disposed.value = 1
            }
        }
    }
}
