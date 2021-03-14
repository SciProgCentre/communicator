package space.kscience.communicator.api

/**
 * Represents abstract factory providing [Transport] objects by name of protocol.
 */
public fun interface TransportFactory {
    /**
     * Returns transport for according [protocol] or `null`.
     */
    public operator fun get(protocol: String): Transport?
}
