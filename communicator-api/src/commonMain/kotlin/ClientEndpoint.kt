package space.kscience.communicator.api

/**
 * Represents communicator client end-point, i.e. pair of protocol and address.
 *
 * @property protocol The transport protocol identifier.
 * @property host The host component.
 * @property port The port component.
 */
public data class ClientEndpoint(val protocol: String, val host: String, val port: Int)

/**
 * Creates [ServerEndpoint] by given protocol identifier and address string.
 *
 * @param address address string in the format "host:port". For example, "localhost:1234" or "127.0.0.1:4321".
 */
public fun ClientEndpoint(protocol: String, address: String): ClientEndpoint =
    ClientEndpoint(protocol, address.split(":")[0], address.split(":")[1].toInt())

/**
 * Represents communicator server end-point, i.e. pair of protocol and port.
 *
 * @property protocol The transport protocol identifier.
 * @property port The port component.
 */
public data class ServerEndpoint(val protocol: String, val port: Int)

/**
 * Creates [ServerEndpoint] by given protocol identifier and address string.
 *
 * @param address address string in the format "host:port". For example, "localhost:1234" or "127.0.0.1:4321". The host
 * part is thrown away, so "anything:1234" produces ServerEndpoint(<protocol>, 1234).
 */
public fun ServerEndpoint(protocol: String, address: String): ServerEndpoint =
    ServerEndpoint(protocol, address.split(":")[1].toInt())

/**
 * Drops host from the given [ClientEndpoint] to create [ServerEndpoint].
 */
public fun ClientEndpoint.toServerEndpoint(): ServerEndpoint = ServerEndpoint(protocol, port)
