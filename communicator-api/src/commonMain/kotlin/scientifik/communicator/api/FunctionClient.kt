package scientifik.communicator.api

interface FunctionClient {
    fun resolveBinaryFunction(endpoint: Endpoint, name: String): BinaryFunction

    fun <T, R> resolveFunction(endpoint: Endpoint, name: String, spec: FunctionSpec<T, R>): suspend (T) -> R =
        resolveBinaryFunction(endpoint, name).toFunction(spec)
}