package scientifik.communicator.api

import kotlinx.io.Closeable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Represents function client that is able to provide named functions for given endpoint and specification.
 */
interface FunctionClient : Closeable {
    /**
     * Constructs a `suspend` function that calls the function server.
     *
     * @param endpoint the endpoint of server.
     * @param name the name of function.
     * @param spec the spec of function.
     */
    fun <T, R> getFunction(endpoint: Endpoint, name: String, spec: FunctionSpec<T, R>): suspend (T) -> R

    /**
     * Disposes this function client.
     */
    override fun close()
}

/**
 * Returns object that uses [FunctionClient.getFunction] to receive function object. The name of function is equal
 * to name of property.
 *
 * @param endpoint the endpoint of server.
 * @param spec the spec of function.
 */
fun <T, R> function(
    endpoint: Endpoint,
    spec: FunctionSpec<T, R>
): ReadOnlyProperty<FunctionClient, suspend (T) -> R> = object : ReadOnlyProperty<FunctionClient, suspend (T) -> R> {
    override fun getValue(thisRef: FunctionClient, property: KProperty<*>): suspend (T) -> R =
        thisRef.getFunction(endpoint, property.name, spec)
}
