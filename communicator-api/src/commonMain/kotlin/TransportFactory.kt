package space.kscience.communicator.api

/**
 * Creates [TransportClient] and [TransportServer] objects for given protocol.
 */
public interface TransportFactory {
    /**
     * Returns [TransportClient] for given protocol identifier.
     */
    public fun client(protocol: String): TransportClient?

    /**
     * Returns [TransportServer] for given port and protocol.
     */
    public fun server(protocol: String, port: Int): TransportServer?

    public companion object : TransportFactory {
        override fun client(protocol: String): TransportClient? = null
        override fun server(protocol: String, port: Int): TransportServer? = null
    }
}
