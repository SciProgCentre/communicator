package space.kscience.communicator.transport

import space.kscience.communicator.api.*
import space.kscience.communicator.zmq.server.ZmqTransportServer
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Function server that can use multiple transport servers to listen to requests using multiple protocols.
 * Host part of endpoint address is ignored, server can work only on local host.
 * Multiple endpoints with the same protocol are ignored, only one port for each protocol is supported.
 * Multiple protocols on one port are not supported.
 */
public class TransportFunctionServer(override val endpoints: Set<Endpoint>) : FunctionServer {
    public constructor(vararg endpoints: Endpoint) : this(endpoints.toSet())

    private val transportServers: List<TransportServer>

    init {
        val actualEndpoints = endpoints
            .asSequence()
            .map { Endpoint(it.protocol, ":${it.port}") }
            .mapTo(hashSetOf()) { it.protocol to it.port }

        check(actualEndpoints.size == endpoints.size) { "Invalid endpoints list. Read docs for DefaultFunctionServer." }

        transportServers = actualEndpoints.map { (protocol, port) ->
            when (protocol) {
                "ZMQ" -> ZmqTransportServer(port)
                else -> error("Protocol $protocol is not supported.")
            }
        }
    }

    override fun <T, R> register(
        name: String,
        spec: FunctionSpec<T, R>,
        function: suspend (T) -> R
    ): suspend (T) -> R {
        val payloadFunction = function.toBinary(spec)
        transportServers.forEach { it.register(name, payloadFunction, spec) }
        return function
    }

    override fun unregister(name: String): Unit = transportServers.forEach { it.unregister(name) }
    override fun close(): Unit = transportServers.forEach(TransportServer::close)
}

public inline fun <S> TransportFunctionServer(
    set: S,
    action: S.(TransportFunctionServer) -> Unit
): TransportFunctionServer where S : FunctionSet {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    val t = TransportFunctionServer(set.endpoint)
    return t.configure(set, action)
}