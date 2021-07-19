package space.kscience.communicator.prettyapi

import io.ktor.utils.io.core.Closeable
import space.kscience.communicator.api.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

@PublishedApi
internal val JSON_LAMBDA: (KClass<*>) -> Codec<*> = { JsonCodec(it) }

@PublishedApi
internal val CBOR_LAMBDA: (KClass<*>) -> Codec<*> = { CborCodec(it) }

@Suppress("UNUSED_PARAMETER")
public class CommunicatorContext(@PublishedApi internal val defaultEndpoint: ClientEndpoint) : Closeable {
    public inline val JSON: (KClass<*>) -> Codec<*>
        get() = JSON_LAMBDA

    public inline val CBOR: (KClass<*>) -> Codec<*>
        get() = CBOR_LAMBDA

    public var serializableFormat: (KClass<*>) -> Codec<*> = JSON

    @PublishedApi
    internal var defaultTransport: TransportFactory = TransportFactory

    private val closeable = mutableListOf<Closeable>()

    private val codecs = arrayOf<Codec<*>>()

    public object CodecsContext {
        public inline fun <reified T : Any> register(codec: Codec<T>): Unit =
            throw NotImplementedError("Implemented as intrinsic")
    }

    public inline fun codecs(action: CodecsContext.() -> Unit): Unit = CodecsContext.action()

    public inline fun <reified T : Any> functionServer(
        implementation: T,
        transport: TransportFactory = defaultTransport,
        endpoint: ClientEndpoint = defaultEndpoint,
    ): Unit = throw NotImplementedError("Implemented as intrinsic")

    public inline fun <reified T : Any> functionClient(
        transport: TransportFactory = defaultTransport,
        endpoint: ClientEndpoint = defaultEndpoint,
    ): T = throw NotImplementedError("Implemented as intrinsic")

    /**
     * Stops and disposes function servers and clients introduced in this context.
     */
    override fun close(): Unit = closeable.forEach(Closeable::close)
}

public inline fun CommunicatorContext.defaultTransport(action: TransportFactory.() -> TransportFactory) {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    defaultTransport = defaultTransport.action()
}

public inline fun <R> communicator(defaultEndpoint: ClientEndpoint, action: CommunicatorContext.() -> R): R {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    return CommunicatorContext(defaultEndpoint).use(action)
}
