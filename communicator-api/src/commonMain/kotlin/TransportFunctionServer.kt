package space.kscience.communicator.api

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Function server implementation based on [TransportServer] objects provided by [TransportServerFactory].
 */
public class TransportFunctionServer(
    private val factory: TransportServerFactory = TransportServerFactory,
    override val endpoints: Set<ServerEndpoint>,
) : FunctionServer {
    public constructor(factory: TransportServerFactory, vararg endpoints: ServerEndpoint) :
            this(factory, endpoints.toSet())

    private val transportServers: List<TransportServer> = endpoints.map { (protocol, port) ->
        factory[protocol, port] ?: error("Protocol $protocol is not supported.")
    }

    public override fun <T, R> register(
        name: String,
        spec: FunctionSpec<T, R>,
        function: suspend (T) -> R,
    ): suspend (T) -> R {
        val payloadFunction = function.toBinary(spec)
        transportServers.forEach { it.register(name, payloadFunction, spec) }
        return function
    }

    public override fun unregister(name: String): Unit = transportServers.forEach { it.unregister(name) }
    public override fun close(): Unit = transportServers.forEach(TransportServer::close)
}

public inline fun <S> TransportFunctionServer(
    set: S,
    factory: TransportServerFactory,
    action: S.(TransportFunctionServer) -> Unit,
): TransportFunctionServer where S : FunctionSet {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    val t = TransportFunctionServer(factory, set.endpoint.toServerEndpoint())
    return t.configure(set, action)
}