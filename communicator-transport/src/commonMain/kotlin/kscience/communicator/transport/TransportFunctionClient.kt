package kscience.communicator.transport

import kscience.communicator.api.*

/**
 * Function client implementation based on [Transport] object provided by [TransportFactory].
 */
public class TransportFunctionClient(private val factory: TransportFactory = DefaultTransportFactory) : FunctionClient {
    private val transportCache: MutableMap<String, Transport> = hashMapOf()

    public override fun <T, R> getFunction(
        endpoint: Endpoint,
        name: String,
        spec: FunctionSpec<T, R>
    ): suspend (T) -> R = transportCache
        .getOrPut(endpoint.protocol) {
            factory[endpoint.protocol] ?: error("Protocol ${endpoint.protocol} is not supported by this client.")
        }
        .channel(endpoint.address, name)
        .toFunction(spec)

    public override fun close(): Unit = transportCache.values.forEach(Transport::close)
}
