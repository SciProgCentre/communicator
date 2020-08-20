package scientifik.communicator.api

import kotlinx.io.Closeable

interface TransportServer : Closeable {
    val port: Int

    suspend fun register(name: String, function: PayloadFunction)
    suspend fun unregister(name: String)
}

interface FunctionServer : Closeable {
    val endpoints: List<Endpoint>

    suspend fun <T, R> register(name: String, spec: FunctionSpec<T, R>, function: suspend (T) -> R): suspend (T) -> R
    suspend fun unregister(name: String)
}
