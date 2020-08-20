package scientifik.communicator.api

import kotlinx.io.Closeable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface FunctionClient : Closeable {
    fun <T, R> getFunction(endpoint: Endpoint, name: String, spec: FunctionSpec<T, R>): suspend (T) -> R
}

fun <T, R> function(
    endpoint: Endpoint,
    spec: FunctionSpec<T, R>
): ReadOnlyProperty<FunctionClient, suspend (T) -> R> = object : ReadOnlyProperty<FunctionClient, suspend (T) -> R> {
    private var function: (suspend (T) -> R)? = null

    override fun getValue(thisRef: FunctionClient, property: KProperty<*>): suspend (T) -> R = function ?: let {
        val new = thisRef.getFunction(endpoint, property.name, spec)
        function = new
        new
    }
}

class TransportFunctionClient(private val factory: TransportFactory) : FunctionClient, Closeable {
    private val transportCache: MutableMap<String, Transport> = hashMapOf()

    override fun <T, R> getFunction(endpoint: Endpoint, name: String, spec: FunctionSpec<T, R>): suspend (T) -> R =
        transportCache
            .getOrPut(endpoint.protocol) {
                factory[endpoint.protocol] ?: error("Protocol ${endpoint.protocol} is not supported by this client.")
            }
            .channel(endpoint.address, name)
            .toFunction(spec)

    override fun close(): Unit = transportCache.values.forEach { it.close() }
}
