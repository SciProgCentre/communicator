package space.kscience.communicator.rsocket


import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.rsocket.kotlin.transport.ktor.server.RSocketSupport
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.utils.io.core.*
import io.ktor.websocket.*
import io.rsocket.kotlin.payload.*
import io.rsocket.kotlin.RSocketRequestHandler
import io.rsocket.kotlin.core.RSocketServer
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.transport.ktor.server.rSocket
import space.kscience.communicator.api.IntCoder
import space.kscience.communicator.api.Payload
import space.kscience.communicator.zmq.client.ZmqTransport
import transport.RSocketPayload


/**
 * Reroute compute requests from RSocket to ZMQ
 */
public class JsProxyServer(
    host: String, port: Int, private val transport: ZmqTransport, private val functionToAddress: Map<String, String>
) {
    private val server = embeddedServer(CIO, host = host, port = port) {
        install(WebSockets)
        install(RSocketSupport)
        routing {
            rSocket("/") {
                RSocketRequestHandler {
                    requestResponse { request: RSocketPayload ->
                        println("PROXY: Got request!")
                        val meta = request.metadata?.readUTF8Line()
                        val splitted = meta?.split(":")
                        val type = splitted?.get(0)
                        println("PROXY: Request type is $type")
                        when(type) {
                            Protocol.Query -> {
                                val id = splitted[1]
                                println("id is $id")
                                //TODO for now name cannot include ':'
                                val name = splitted[2]
                                println("name is $name")
                                //TODO decide what to do if address is unknown or how to get new addresses
                                val address = functionToAddress[name]!!
                                println("address is $address, arg is ${IntCoder.decode(request.data.readBytes())}")
                                val binResponse = transport.respond(address, name, request.data.readBytes())
                                println("got response!")
                                buildPayload {
                                    data(binResponse)
                                    metadata("${Protocol.Response.Result}:$id")
                                }
                            }

                            else -> {
                                buildPayload {
                                    data("")
                                    metadata("${Protocol.Response.UnexpectedError}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    public fun start() {
        server.start()
    }
    public fun stop() {
        server.stop(100, 100)
    }
}