package transport

import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.utils.io.core.*
import io.rsocket.kotlin.RSocketRequestHandler
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.transport.ktor.server.RSocketSupport
import io.rsocket.kotlin.transport.ktor.server.rSocket
import space.kscience.communicator.api.Transport
import io.ktor.application.*

public actual class RSocketServer public actual constructor(port: Int,
                                                            transport: Transport) {
    private val server = embeddedServer(CIO, port = port) {
        install(RSocketSupport)
        routing {
            rSocket() {
                RSocketRequestHandler {
                    requestResponse { request ->
                        val requestType = request.data.readByte()
                        when (requestType) {
                            RequestType.Compute -> {
                                val address = request.data.readUTF8Line()
                                val name = request.data.readUTF8Line()
                                val payload = request.data.readBytes()

                                val answer = address?.let { name?.let { it1 -> transport.respond(it, it1, payload) } }
                                if (answer == null) {
                                    println("oh no...")
                                    Payload(ByteReadPacket(byteArrayOf()))
                                } else {
                                    io.rsocket.kotlin.payload.Payload(ByteReadPacket(answer))
                                }
                            }
                            else -> {
                                Payload(ByteReadPacket(byteArrayOf()))
                            }
                        }
                    }
                }
            }
        }
    }

    public actual fun start() {
        server.start()
    }
}