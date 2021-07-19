package space.kscience.communicator.api

import io.ktor.utils.io.core.Closeable
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Function server implementation based on [TransportServer] objects provided by [TransportFactory].
 *
 * @property endpoints The set of endpoints this class serves.
 */
public class FunctionServer(
    private val factory: TransportFactory = TransportFactory,
    public val endpoints: Set<ServerEndpoint>,
) : Closeable {
    public constructor(factory: TransportFactory, vararg endpoints: ServerEndpoint) :
            this(factory, endpoints.toSet())

    internal val transportServers: List<TransportServer> = endpoints.map { endpoint ->
        factory.server(endpoint.protocol, endpoint.port) ?: error("Protocol ${endpoint.protocol} is not supported.")
    }

    /**
     * Registers a function in this server.
     *
     * @param T the type the function takes.
     * @param R the type the function returns.
     * @param name the name of function.
     * @param argumentCodec the codec of [T].
     * @param resultCodec the codec of [R].
     * @param function the function implementation.
     * @return the function implementation.
     */
    public fun <T : Any, R : Any> register(
        name: String,
        argumentCodec: Codec<T>,
        resultCodec: Codec<R>,
        function: suspend (T) -> R,
    ) {
        val payloadFunction = function.toBinary(argumentCodec, resultCodec)
        transportServers.forEach { it.register(name, payloadFunction, argumentCodec, resultCodec) }
    }

    /**
     * Unregisters a function from this server.
     *
     * @param name the name of function.
     */
    public fun unregister(name: String): Unit = transportServers.forEach { it.unregister(name) }

    /**
     * Stops and disposes this function server.
     */
    override fun close(): Unit = transportServers.forEach(TransportServer::close)
}

/**
 * Constructs [FunctionServer] in the context of [S].
 */
public inline fun <S> FunctionServer(
    set: S,
    factory: TransportFactory,
    action: S.(FunctionServer) -> Unit,
): FunctionServer where S : FunctionSet {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    val t = FunctionServer(factory, set.endpoint)
    return t.configure(set, action)
}
