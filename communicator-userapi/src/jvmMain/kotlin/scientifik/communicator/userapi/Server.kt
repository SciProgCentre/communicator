package scientifik.communicator.userapi

import scientifik.communicator.api.Coder
import scientifik.communicator.userapi.transport.ServerTransport
import java.io.Closeable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

abstract class Server(endpoint: String) : Closeable {
    private val transport = ServerTransport(endpoint)

    fun <T> function(coder: Coder<T>, f: (T) -> T): ReadOnlyProperty<Server, F<T>> = FunctionDelegate(coder, f)

    fun start() {
        javaClass.kotlin.memberProperties.forEach {
            it.isAccessible = true
            it.get(this)
        }

        transport.start()
    }

    private class FunctionDelegate<T>(private val coder: Coder<T>, private val f: (T) -> T) :
        ReadOnlyProperty<Server, F<T>> {
        private var isInitialized = false

        override operator fun getValue(thisRef: Server, property: KProperty<*>): F<T> {
            if (!isInitialized) {
                thisRef.transport.functions[property.name] = { coder.encode(f(coder.decode(it))) }
                isInitialized = true
            }

            return F(f)
        }
    }

    class F<T> internal constructor(val lambda: (T) -> T) : (T) -> T {
        override fun invoke(p1: T): T = lambda(p1)
    }

    override fun close(): Unit = transport.close()
}