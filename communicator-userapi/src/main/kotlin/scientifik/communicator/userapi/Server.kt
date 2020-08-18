package scientifik.communicator.userapi

import scientifik.communicator.api.Coder
import scientifik.communicator.userapi.transport.ServerTransport
import java.io.Closeable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class Server(endpoint: String) : Closeable {
    private val transport = ServerTransport(endpoint)

    fun <T> function(name: String, coder: Coder<T>, f: (T) -> T): ReadOnlyProperty<Server, F<T>> {
        transport.functions[name] = { coder.encode(f(coder.decode(it))) }
        return FunctionDelegate(coder, f)
    }

    init {
        transport.start()
    }

    private class FunctionDelegate<T>(private val coder: Coder<T>, private val f: (T) -> T) :
        ReadOnlyProperty<Server, F<T>> {
        override operator fun getValue(thisRef: Server, property: KProperty<*>): F<T> = F(coder, f)
    }

    class F<T> internal constructor(val coder: Coder<T>, val lambda: (T) -> T) : (T) -> T {
        override fun invoke(p1: T): T = lambda(p1)
    }

    override fun close(): Unit = transport.close()
}