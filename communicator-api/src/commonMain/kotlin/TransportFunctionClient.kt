package space.kscience.communicator.api

/**
 * Function client implementation based on [TransportClient] objects provided by [TransportClientFactory].
 */
public class TransportFunctionClient(private val factory: TransportClientFactory) :
    FunctionClient {
    private val transportCache: MutableMap<String, TransportClient> = hashMapOf()

    public override fun <T, R> getFunction(
        endpoint: ClientEndpoint,
        name: String,
        spec: FunctionSpec<T, R>,
    ): suspend (T) -> R = transportCache
        .getOrPut(endpoint.protocol) {
            factory[endpoint.protocol] ?: error("Protocol ${endpoint.protocol} is not supported by this client.")
        }
        .channel(endpoint.host, endpoint.port, name)
        .toFunction(spec)

    public override fun close(): Unit = transportCache.values.forEach(TransportClient::close)
}
