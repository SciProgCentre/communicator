package space.kscience.communicator.api

/**
 * Creates [TransportServer] for given port and protocol.
 */
public fun interface TransportServerFactory {
    /**
     * Returns [TransportServer] for given port and protocol.
     */
    public operator fun get(protocol: String, port: Int): TransportServer?

    public companion object : TransportServerFactory {
        public override fun get(protocol: String, port: Int): TransportServer? = null
    }
}
