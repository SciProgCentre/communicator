package transport

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.rsocket.kotlin.transport.ktor.client.RSocketSupport
import io.rsocket.kotlin.transport.ktor.client.rSocket
import space.kscience.communicator.api.Payload


public class RSocketClient(private val host: String, private val port: Int) {
    private val client: HttpClient = HttpClient(CIO) {
        install(WebSockets)
        install(RSocketSupport)
    }

    public suspend fun respond(payload: Payload): Payload {
        val rSocket = client.rSocket(host = host, port = port)
        val answer = rSocket.requestResponse(io.rsocket.kotlin.payload.Payload(ByteReadChannel(payload).readPacket(payload.size)))
        val answerPayload = Payload(answer.data.remaining.toInt())
        answer.data.readFully(answerPayload)
        return answerPayload
    }
}