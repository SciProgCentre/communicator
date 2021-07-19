package space.kscience.communicator.prettyapi

import io.ktor.utils.io.core.Closeable
import space.kscience.communicator.api.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod

@PublishedApi
internal val JSON_LAMBDA: (KClass<*>) -> Codec<*> = { JsonCodec(it) }

@PublishedApi
internal val CBOR_LAMBDA: (KClass<*>) -> Codec<*> = { CborCodec(it) }

public class CommunicatorContext(@PublishedApi internal val defaultEndpoint: ClientEndpoint) : Closeable {
    public inline val JSON: (KClass<*>) -> Codec<*>
        get() = JSON_LAMBDA

    public inline val CBOR: (KClass<*>) -> Codec<*>
        get() = CBOR_LAMBDA

    public var serializableFormat: (KClass<*>) -> Codec<*> = JSON

    @PublishedApi
    internal var defaultTransport: TransportFactory = TransportFactory

    internal val codecsRegistry: MutableMap<KClass<*>, Codec<*>> = hashMapOf(
        Unit::class to UnitCodec,
        Int::class to IntCodec,
        Long::class to LongCodec,
        ULong::class to ULongCodec,
        Float::class to FloatCodec,
        Double::class to DoubleCodec,
        String::class to StringCodec,
    )

    public fun <T : Any> registerCodec(classOfT: KClass<T>, codec: Codec<T>) {
        require(classOfT !in codecsRegistry) { "Codec for $classOfT is already present in the registry." }
        codecsRegistry[classOfT] = codec
    }

    private val closeable = mutableListOf<Closeable>()

    @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "UNCHECKED_CAST")
    public fun <T : Any> functionServer(
        classOfT: KClass<T>,
        implementation: T,
        transport: TransportFactory = defaultTransport,
        endpoint: ClientEndpoint = defaultEndpoint,
    ) {
        val metadata = scanInterface(classOfT, endpoint)

        val f = FunctionServer(metadata.functionSet, transport) {
            metadata.mapping.forEach { (function, declaration) ->
                val f: suspend (Any) -> Any = when (function.valueParameters.size) {
                    0 -> { _ -> function.callSuspend(implementation)!! }
                    1 -> { arg -> function.callSuspend(implementation, arg)!! }
                    else -> { arg -> function.callSuspend(implementation, *(arg as List<Any>).toTypedArray())!! }
                }

                val payloadFunction =
                    f.toBinary(declaration.argumentCodec as Codec<Any>, declaration.resultCodec as Codec<Any>)

                it.transportServers.forEach {
                    it.register(
                        declaration.name,
                        payloadFunction,
                        declaration.argumentCodec,
                        declaration.resultCodec,
                    )
                }
            }
        }

        closeable += f
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> functionClient(
        classOfT: KClass<T>,
        transport: TransportFactory = defaultTransport,
        endpoint: ClientEndpoint = defaultEndpoint,
    ): T {
        val client = FunctionClient(transport)
        closeable += client
        val metadata = scanInterface(classOfT, endpoint)
        return makeObject(classOfT, client, endpoint, metadata) as T
    }

    /**
     * Stops and disposes function servers and clients introduced in this context.
     */
    override fun close(): Unit = closeable.forEach(Closeable::close)
}

public inline fun CommunicatorContext.defaultTransport(action: TransportFactory.() -> TransportFactory) {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    defaultTransport = defaultTransport.action()
}

public inline fun <reified T : Any> CommunicatorContext.registerCodec(codec: Codec<T>): Unit =
    registerCodec(T::class, codec)

public inline fun <reified T : Any> CommunicatorContext.functionServer(
    implementation: T,
    transport: TransportFactory = defaultTransport,
    endpoint: ClientEndpoint = defaultEndpoint,
): Unit = functionServer(T::class, implementation, transport, endpoint)

public inline fun <reified T : Any> CommunicatorContext.functionClient(
    transport: TransportFactory = defaultTransport,
    endpoint: ClientEndpoint = defaultEndpoint,
): T = functionClient(T::class, transport, endpoint)

public inline fun <R> communicator(defaultEndpoint: ClientEndpoint, action: CommunicatorContext.() -> R): R {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    return CommunicatorContext(defaultEndpoint).use(action)
}
