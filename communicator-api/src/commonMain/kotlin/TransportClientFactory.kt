package space.kscience.communicator.api

/**
 * Creates [TransportClient] for given protocol identifier.
 */
public fun interface TransportClientFactory {
    /**
     * Returns [TransportClient] for given protocol identifier.
     */
    public operator fun get(protocol: String): TransportClient?

    public companion object : TransportClientFactory {
        public override fun get(protocol: String): TransportClient? = null
    }
}
