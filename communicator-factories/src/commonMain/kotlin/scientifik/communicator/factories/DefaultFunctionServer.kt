package scientifik.communicator.factories

import scientifik.communicator.api.*
import scientifik.communicator.zmq.server.ZMQTransportServer

/**
 * Function server that can use multiple transport servers to listen to requests using multiple protocols.
 * Host part of endpoint address is ignored, server can work only on local host.
 * Multiple endpoints with the same protocol are ignored, only one port for each protocol is supported.
 * Multiple protocols on one port are not supported.
 */
class DefaultFunctionServer(override val endpoints: List<Endpoint>) : FunctionServer {

    private val transportServers: List<TransportServer>

    init {
        val actualEndpoints = endpoints
                .distinctBy { it.protocol }
                .distinctBy { it.port }
                .map { it.protocol to it.port }
        if (actualEndpoints.size != endpoints.size) error("Invalid endpoints list. Read docs for DefaultFunctionServer.")
        transportServers = actualEndpoints.map {
            val (protocol, port) = it
            when (protocol) {
                "ZMQ" -> ZMQTransportServer(port)
                else -> error("Protocol $protocol is not supported.")
            }
        }
    }

    override fun <T, R> register(name: String, spec: FunctionSpec<T, R>, function: suspend (T) -> R) {
        val payloadFunction = function.toBinary(spec)
        transportServers.forEach {
            it.register(name, payloadFunction)
        }
    }

    override fun unregister(name: String) {
        transportServers.forEach {
            it.unregister(name)
        }
    }

    override fun stop() {
        //TODO
    }

}