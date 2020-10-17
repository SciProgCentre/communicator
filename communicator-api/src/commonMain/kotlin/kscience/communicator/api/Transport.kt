package kscience.communicator.api

import kotlinx.io.Closeable

/**
 * A transport provider for binary endpoints.
 */
public interface Transport : Closeable {
    /**
     * Communicates with endpoint by transceiving a payload.
     *
     * @param address the address to channel.
     * @param name the name of function.
     * @param payload the payload to send.
     * @return the received payload.
     */
    public suspend fun respond(address: String, name: String, payload: Payload): Payload

    /**
     * Returns a payload function channeling this transport.
     *
     * @param address the address to channel.
     * @param name the name of function.
     * @return the freshly created function.
     */
    public fun channel(address: String, name: String): PayloadFunction = { arg -> respond(address, name, arg) }

    /**
     * Disposes this transport.
     */
    override fun close()
}
