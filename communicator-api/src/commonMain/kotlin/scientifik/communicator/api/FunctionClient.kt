package scientifik.communicator.api

interface FunctionClient {
    fun <T, R> function(endpoint: Endpoint, name: String, spec: FunctionSpec<T, R>): suspend (T) -> R
}

class TransportFunctionClient(private val factory: TransportFactory) : FunctionClient {
    override fun <T, R> function(endpoint: Endpoint, name: String, spec: FunctionSpec<T, R>): suspend (T) -> R {
        val transport = factory[endpoint.protocol] ?: error("Protocol ${endpoint.protocol} is not supported by this client.")
        return transport.channel(endpoint.address, name).toFunction(spec)
    }
}