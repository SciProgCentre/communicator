package space.kscience.communicator.api

/**
 * Represents communicator client end-point i.e., a triple of protocol, host, and port.
 *
 * @property host The host.
 */
public class ClientEndpoint(protocol: String, public val host: String, port: Int) :
    ServerEndpoint(protocol, port, false) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClientEndpoint) return false
        if (!super.equals(other)) return false

        if (host != other.host) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + host.hashCode()
        return result
    }

    override fun toString(): String = "ClientEndpoint(host='$host') ${super.toString()}"
}

/**
 * Creates [ClientEndpoint] by given protocol identifier and address string.
 *
 * @param address address string in the format "host:port". For example, "localhost:1234" or "127.0.0.1:4321".
 */
public fun ClientEndpoint(protocol: String, address: String): ClientEndpoint =
    ClientEndpoint(protocol, address.split(":")[0], address.split(":")[1].toInt())

/**
 * Represents communicator server end-point i.e., a pair of protocol and port.
 *
 * @property protocol The identifier of the transport protocol.
 * @property port The port.
 */
public open class ServerEndpoint internal constructor(
    public val protocol: String,
    public open val port: Int,
    @Suppress("UNUSED_PARAMETER") dummy: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ServerEndpoint) return false

        if (protocol != other.protocol) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = protocol.hashCode()
        result = 31 * result + port
        return result
    }

    override fun toString(): String = "ServerEndpoint(protocol='$protocol', port=$port)"
}

public fun ServerEndpoint(protocol: String, port: Int): ServerEndpoint = ServerEndpoint(protocol, port, false)


/**
 * Creates [ServerEndpoint] by given protocol identifier and address string.
 *
 * @param address address string in the format "host:port". For example, "localhost:1234" or "127.0.0.1:4321". The host
 * part is thrown away, so "anything:1234" produces ServerEndpoint(<protocol>, 1234).
 */
public fun ServerEndpoint(protocol: String, address: String): ServerEndpoint =
    ServerEndpoint(protocol, address.split(":")[1].toInt())
