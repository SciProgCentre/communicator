package space.kscience.communicator.api

import io.ktor.utils.io.core.Closeable
import kotlin.properties.ReadOnlyProperty

/**
 * Represents function client that is able to provide named functions for given endpoint and specification.
 *
 * Implementation is based on [TransportClient] objects provided by [TransportFactory].
 */
public class FunctionClient(internal val factory: TransportFactory) : Closeable {
    internal val transportCache: MutableMap<String, TransportClient> = hashMapOf()

    /**
     * Constructs a `suspend` function that calls the function server.
     *
     * @param endpoint the endpoint of server.
     * @param name the name of function.
     * @param argumentCodec the codec of [T].
     * @param resultCodec the codec of [R].
     * @return the result function.
     */
    public fun <T : Any, R : Any> getFunction(
        endpoint: ClientEndpoint,
        name: String,
        argumentCodec: Codec<T>,
        resultCodec: Codec<R>,
    ): suspend (T) -> R = transportCache
        .getOrPut(endpoint.protocol) {
            factory.client(endpoint.protocol) ?: error("Protocol ${endpoint.protocol} is not supported by this client.")
        }
        .channel(endpoint.host, endpoint.port, name)
        .toFunction(argumentCodec, resultCodec)

    /**
     * Disposes this function client.
     */
    override fun close(): Unit = transportCache.values.forEach(TransportClient::close)
}

/**
 * Returns object that uses [FunctionClient.getFunction] to receive function object. The name of function is equal
 * to name of property.
 *
 * @receiver the client to get function from.
 * @param endpoint the endpoint of server.
 * @param argumentCodec the codec of [T].
 * @param resultCodec the codec of [R].
 */
public fun <T : Any, R : Any> FunctionClient.function(
    endpoint: ClientEndpoint,
    argumentCodec: Codec<T>,
    resultCodec: Codec<R>,
): ReadOnlyProperty<Any?, suspend (T) -> R> =
    ReadOnlyProperty { _, property -> getFunction(endpoint, property.name, argumentCodec, resultCodec) }
