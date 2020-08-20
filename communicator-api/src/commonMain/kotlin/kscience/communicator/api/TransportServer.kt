package kscience.communicator.api

import kotlinx.io.Closeable

/**
 * Represents low-level server serving [PayloadFunction] objects.
 */
interface TransportServer : Closeable {
    /**
     * The port this transport server is bound.
     */
    val port: Int

    /**
     * Registers a named payload function.
     *
     * @param name the name of function.
     * @param function the implementation of function.
     */
    suspend fun register(name: String, function: PayloadFunction)

    /**
     * Unregisters function by its name.
     *
     * @param name the name of function.
     */
    suspend fun unregister(name: String)

    /**
     * Stops and disposes this transport server.
     */
    override fun close()
}
