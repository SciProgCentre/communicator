package scientifik.communicator.userapi

import kotlinx.coroutines.runBlocking
import scientifik.communicator.api.Coder
import scientifik.communicator.userapi.transport.ClientTransport
import java.io.Closeable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class Client : Closeable {
    private val transport = ClientTransport()

    fun <T> function(coder: Coder<T>): ReadOnlyProperty<Client, F<T>> = FunctionDelegate(coder)

    private class FunctionDelegate<T>(private val coder: Coder<T>) : ReadOnlyProperty<Client, F<T>> {
        override operator fun getValue(thisRef: Client, property: KProperty<*>): F<T> {
            val name = property.name
            var f: F<T>? = null

            runBlocking {
                val id = thisRef.transport.coderIDAsync(name).await()
                requireNotNull(id)
                f = F(name, thisRef, coder)
            }

            return f!!
        }
    }

    class F<T> internal constructor(
        private val name: String,
        private val client: Client,
        private val tCoder: Coder<T>
    ) {
        suspend operator fun invoke(arg: T): T =
            tCoder.decode(client.transport.evaluateAsync(name, tCoder.encode(arg)).await())
    }

    override fun close(): Unit = transport.close()
}
