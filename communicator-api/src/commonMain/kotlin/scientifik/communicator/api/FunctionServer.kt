package scientifik.communicator.api

import kotlinx.io.Closeable

interface TransportServer : Closeable {
    val port: Int

    fun register(name: String, function: PayloadFunction)
    fun unregister(name: String)
}

interface FunctionServer : Closeable {
    val endpoints: List<Endpoint>

    fun <T, R> register(name: String, spec: FunctionSpec<T, R>, function: suspend (T) -> R): suspend (T) -> R

    fun unregister(name: String)
}
