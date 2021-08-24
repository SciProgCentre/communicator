package space.kscience.communicator.api

import io.ktor.utils.io.core.Closeable

/**
 * A transport provider for binary endpoints.
 */
public interface TransportClient : Closeable {
    /**
     * Communicates with endpoint by transceiving a payload.
     *
     * @param host the host to channel.
     * @param port the port to channel.
     * @param name the name of function.
     * @param payload the payload to send.
     * @return the received payload.
     */
    public suspend fun respond(host: String, port: Int, name: String, payload: Payload): Payload

    /**
     * Returns a payload function channeling this transport.
     *
     * @param host the host to channel.
     * @param port the port to channel.
     * @param name the name of function.
     * @return the freshly created function.
     */
    public fun channel(host: String, port: Int, name: String): PayloadFunction =
        { arg -> respond(host, port, name, arg) }

    /**
     * Disposes this transport.
     */
    override fun close()
}
