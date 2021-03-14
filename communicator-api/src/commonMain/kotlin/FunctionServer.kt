package space.kscience.communicator.api

import io.ktor.utils.io.core.Closeable

/**
 * Represents communicator function server that is able to register and unregister functions to serve them at several
 * endpoints.
 */
public interface FunctionServer : Closeable {
    /**
     * The set of endpoints this object serves.
     */
    public val endpoints: Set<Endpoint>

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
    public fun <T, R> register(
        name: String,
        spec: FunctionSpec<T, R>,
        function: suspend (T) -> R
    ): suspend (T) -> R

    /**
     * Unregisters a function from this server.
     *
     * @param name the name of function.
     */
    public fun unregister(name: String)

    /**
     * Stops and disposes this function server.
     */
    public override fun close()
}
