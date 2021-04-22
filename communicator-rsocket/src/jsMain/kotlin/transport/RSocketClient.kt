package transport

import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.rsocket.kotlin.transport.ktor.client.RSocketSupport
import io.rsocket.kotlin.transport.ktor.client.rSocket
import io.ktor.client.engine.js.*


public actual class RSocketClient actual constructor(host: String, port: Int) {
    private val host = host
    private val port = port

    private val client: HttpClient = HttpClient(Js) {
        install(WebSockets)
        install(RSocketSupport)
    }

    public actual suspend fun respond(payload: RSocketPayload): RSocketPayload {
        val rSocket = client.rSocket(host = host, port = port)
        return rSocket.requestResponse(payload)
    }
}
