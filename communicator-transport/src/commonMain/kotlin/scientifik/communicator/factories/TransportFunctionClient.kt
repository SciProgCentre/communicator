package scientifik.communicator.factories

import scientifik.communicator.api.*

class TransportFunctionClient(private val factory: TransportFactory) : FunctionClient {
    private val transportCache: MutableMap<String, Transport> = hashMapOf()

    override fun <T, R> getFunction(endpoint: Endpoint, name: String, spec: FunctionSpec<T, R>): suspend (T) -> R =
        transportCache
            .getOrPut(endpoint.protocol) {
                factory[endpoint.protocol] ?: error("Protocol ${endpoint.protocol} is not supported by this client.")
            }
            .channel(endpoint.address, name)
            .toFunction(spec)

    override fun close(): Unit = transportCache.values.forEach(Transport::close)
}
