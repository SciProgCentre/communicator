package scientifik.communicator.api

import kotlinx.io.use
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface FunctionClient {
    fun <T, R> getFunction(endpoint: Endpoint, name: String, spec: FunctionSpec<T, R>): suspend (T) -> R
}

fun <T, R> function(
    endpoint: Endpoint,
    name: String,
    spec: FunctionSpec<T, R>
): ReadOnlyProperty<FunctionClient, suspend (T) -> R> = object : ReadOnlyProperty<FunctionClient, suspend (T) -> R> {
    private var function: (suspend (T) -> R)? = null

    override fun getValue(thisRef: FunctionClient, property: KProperty<*>): suspend (T) -> R = function ?: let {
        val new = thisRef.getFunction(endpoint, name, spec)
        function = new
        new
    }
}

class TransportFunctionClient(private val factory: TransportFactory) : FunctionClient {
    override fun <T, R> getFunction(endpoint: Endpoint, name: String, spec: FunctionSpec<T, R>): suspend (T) -> R {
        var res: (suspend (T) -> R)? = null

        (factory[endpoint.protocol] ?: error("Protocol ${endpoint.protocol} is not supported by this client.")).use {
            res = it.channel(endpoint.address, name).toFunction(spec)
        }

        return checkNotNull(res)
    }
}
