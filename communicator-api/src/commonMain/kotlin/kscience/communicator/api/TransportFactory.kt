package kscience.communicator.api

/**
 * Represents abstract factory providing [Transport] objects by name of protocol.
 */
interface TransportFactory {
    /**
     * Returns transport for according [protocol] or `null`.
     */
    operator fun get(protocol: String): Transport?
}
