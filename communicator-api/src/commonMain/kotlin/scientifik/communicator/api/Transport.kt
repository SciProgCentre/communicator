package scientifik.communicator.api

import kotlinx.io.Closeable

typealias PayloadFunction = suspend (Payload) -> Payload

/**
 * A transport provider for binary endpoints.
 */
interface Transport : Closeable {
    suspend fun respond(address: String, name: String, payload: Payload): Payload
    fun channel(address: String, name: String): PayloadFunction = { arg -> respond(address, name, arg) }
}

interface TransportFactory {
    operator fun get(protocol: String): Transport?
}
