package transport

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.rsocket.kotlin.transport.ktor.client.RSocketSupport
import io.rsocket.kotlin.transport.ktor.client.rSocket
import space.kscience.communicator.api.Payload


public typealias RSocketPayload = io.rsocket.kotlin.payload.Payload

/**
 * Send a request and receive a reply
 */
public class RSocketClient(private val host: String, private val port: Int) {
    private val client: HttpClient = HttpClient(CIO) {
        install(WebSockets)
        install(RSocketSupport)
    }

    public suspend fun respond(payload: RSocketPayload): RSocketPayload {
        val rSocket = client.rSocket(host = host, port = port)
        return rSocket.requestResponse(payload)
    }
}