package space.kscience.communicator.api

import io.ktor.utils.io.core.Closeable

/**
 * Represents low-level server serving [PayloadFunction] objects.
 */
public interface TransportServer : Closeable {
    /**
     * The port this transport server is bound.
     */
    public val port: Int

    /**
     * Registers a named payload function.
     *
     * @param name the name of function.
     * @param function the implementation of function.
     * @param spec the pair of names of argument and result coders.
     */
    public fun register(name: String, function: PayloadFunction, spec: Pair<String, String>)

    /**
     * Unregisters function by its name.
     *
     * @param name the name of function.
     */
    public fun unregister(name: String)

    /**
     * Stops and disposes this transport server.
     */
    override fun close()
}