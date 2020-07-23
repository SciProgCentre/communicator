package scientifik.communicator.api

typealias BinaryFunction = suspend (Payload) -> Payload

/**
 * A transport provider for binary endpoints.
 */
interface Transport {
    suspend fun respond(address: String, name: String, payload: Payload): Payload
    fun channel(address: String, name: String): BinaryFunction = { arg -> respond(address, name, arg) }
}

interface TransportFactory{
    operator fun get(protocol: String): Transport?
}

