package scientifik.communicator.api

import kotlinx.io.Closeable

/**
 * Represents communicator function server that is able to register and unregister functions to serve them at several
 * endpoints.
 */
interface FunctionServer : Closeable {
    /**
     * The set of endpoints this object serves.
     */
    val endpoints: Set<Endpoint>

    /**
     * Registers a function in this server.
     *
     * @param T the type the function takes.
     * @param R the type the function returns.
     * @param name the name of function.
     * @param spec the spec of function.
     * @param function the function implementation.
     * @return the function implementation.
     */
    suspend fun <T, R> register(name: String, spec: FunctionSpec<T, R>, function: suspend (T) -> R): suspend (T) -> R

    /**
     * Unregisters a function from this server.
     *
     * @param name the name of function.
     */
    suspend fun unregister(name: String)

    /**
     * Stops and disposes this function server.
     */
    override fun close()
}
