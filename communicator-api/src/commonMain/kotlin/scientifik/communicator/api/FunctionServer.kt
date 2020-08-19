package scientifik.communicator.api

interface TransportServer {
    val port: Int

    fun register(name: String, function: PayloadFunction)
    fun unregister(name: String)

    fun stop()
}

interface FunctionServer {
    val endpoints: List<Endpoint>

    fun <T, R> register(name: String, spec: FunctionSpec<T, R>, function: suspend (T) -> R)

    fun unregister(name: String)

    fun stop()
}
