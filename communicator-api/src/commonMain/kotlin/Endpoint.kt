package space.kscience.communicator.api

/**
 * Represents communicator end-point, i.e. pair of protocol and address.
 *
 * @property protocol The transport protocol to find [Transport].
 * @property address Address string in the format "host:port". For example, "localhost:1234" or "127.0.0.1:4321"
 */
public data class Endpoint(val protocol: String, val address: String) {
    /**
     * The host part (before :) of [address].
     */
    val host: String by lazy { address.split(":")[0] }

    /**
     * The port part (after :) of [address].
     */
    val port: Int by lazy { address.split(":")[1].toInt() }
}
