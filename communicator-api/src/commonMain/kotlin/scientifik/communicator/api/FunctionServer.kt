package scientifik.communicator.api

interface FunctionServer {
    val endpoints: List<Endpoint>
    val specs: Map<String, FunctionSpec<*, *>>

    fun <T, R> register(name: String, spec: FunctionSpec<T, R>, function: suspend (T) -> R)
    fun unregister(name: String)
}

interface FunctionServerFactory<C> {
    fun build(configuration: C): FunctionServer
}