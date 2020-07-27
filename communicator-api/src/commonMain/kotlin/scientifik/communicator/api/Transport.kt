package scientifik.communicator.api

typealias PayloadFunction = suspend (Payload) -> Payload

/**
 * A transport provider for binary endpoints.
 */
interface Transport {
    suspend fun respond(address: String, name: String, payload: Payload): Payload
    fun channel(address: String, name: String): PayloadFunction = { arg -> respond(address, name, arg) }
}

interface TransportFactory{
    operator fun get(protocol: String): Transport?
}
