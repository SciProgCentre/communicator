package transport

import io.ktor.utils.io.core.*
import io.rsocket.kotlin.payload.*
import io.rsocket.kotlin.payload.buildPayload
import space.kscience.communicator.api.Endpoint
import space.kscience.communicator.api.Payload
import space.kscience.communicator.api.Transport

public class DummyRSocketTransport: Transport {
    private val clients = mutableMapOf<String, RSocketClient>()
    private var currentId = 0

    override suspend fun respond(address: String, name: String, payload: Payload): Payload {
        val client = clients.getOrPut(address, {
            val endpoint = Endpoint("rsocket", address)
            RSocketClient(endpoint.host, endpoint.port)
        })

        val request = buildPayload {
            data(payload)
            metadata("${Protocol.Query}:$currentId:$name")
        }
        val answer = client.respond(request)
        return answer.data.readBytes()
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}