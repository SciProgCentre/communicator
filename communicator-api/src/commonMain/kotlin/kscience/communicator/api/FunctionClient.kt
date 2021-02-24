package kscience.communicator.api

import io.ktor.utils.io.core.Closeable
import kotlin.properties.ReadOnlyProperty

/**
 * Represents function client that is able to provide named functions for given endpoint and specification.
 */
public interface FunctionClient : Closeable {
    /**
     * Constructs a `suspend` function that calls the function server.
     *
     * @param endpoint the endpoint of server.
     * @param name the name of function.
     * @param spec the spec of function.
     */
    public fun <T, R> getFunction(endpoint: Endpoint, name: String, spec: FunctionSpec<T, R>): suspend (T) -> R

    public fun <T, R> getFunction(remoteFunction: RemoteFunction<T, R>): suspend (T) -> R =
        getFunction(remoteFunction.endpoint, remoteFunction.name, remoteFunction.spec)

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
public fun <T, R> function(
    endpoint: Endpoint,
    spec: FunctionSpec<T, R>
): ReadOnlyProperty<FunctionClient, suspend (T) -> R> =
    ReadOnlyProperty { thisRef, property -> thisRef.getFunction(endpoint, property.name, spec) }
