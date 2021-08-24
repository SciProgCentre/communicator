package space.kscience.communicator.api

import io.ktor.utils.io.*
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

private suspend fun ByteReadChannel.copyAvailable(): ByteArray =
    ByteArray(availableForRead).also { readFully(it) }

private val EMPTY_BYTE_ARRAY = ByteArray(0)
private val UNIT_AND_ZERO = Unit to 0

/**
 * Binds [Unit] to `common/unit` codec.
 */
public object UnitCodec : Codec<Unit> {
    override val identity: String
        get() = "common/unit"

    override suspend fun decode(payload: Payload): Pair<Unit, Int> = UNIT_AND_ZERO
    override suspend fun encode(value: Unit): Payload = EMPTY_BYTE_ARRAY
}

/**
 * Binds [Int] to `common/i32` codec.
 */
public object IntCodec : Codec<Int> {
    override val identity: String
        get() = "common/i32"

    override suspend fun decode(payload: Payload): Pair<Int, Int> = ByteReadChannel(payload).readInt() to Int.SIZE_BYTES
    override suspend fun encode(value: Int): Payload = ByteChannel(true).also { it.writeInt(value) }.copyAvailable()
}

/**
 * Binds [Long] to `common/i64`.
 */
public object LongCodec : Codec<Long> {
    override val identity: String
        get() = "common/i64"

    override suspend fun decode(payload: Payload): Pair<Long, Int> =
        ByteReadChannel(payload).readLong() to Long.SIZE_BYTES

    override suspend fun encode(value: Long): Payload =
        ByteChannel(true).also { it.writeLong(value) }.copyAvailable()
}

/**
 * Binds [ULong] to `common/u64`.
 */
public object ULongCodec : Codec<ULong> {
    override val identity: String
        get() = "common/u64"

    override suspend fun decode(payload: Payload): Pair<ULong, Int> =
        ByteReadChannel(payload).readLong().toULong() to ULong.SIZE_BYTES

    override suspend fun encode(value: ULong): Payload =
        ByteChannel(true).also { it.writeLong(value.toLong()) }.copyAvailable()
}

/**
 * Binds [Float] to `common/f32`.
 */
public object FloatCodec : Codec<Float> {
    override val identity: String
        get() = "common/f32"

    override suspend fun decode(payload: Payload): Pair<Float, Int> =
        ByteReadChannel(payload).readFloat() to Float.SIZE_BYTES

    override suspend fun encode(value: Float): Payload =
        ByteChannel(true).also { it.writeFloat(value) }.copyAvailable()
}

/**
 * Binds [Double] to `common/f64`.
 */
public object DoubleCodec : Codec<Double> {
    override val identity: String
        get() = "common/f64"

    override suspend fun decode(payload: Payload): Pair<Double, Int> =
        ByteReadChannel(payload).readDouble() to Double.SIZE_BYTES

    override suspend fun encode(value: Double): Payload =
        ByteChannel(true).also { it.writeDouble(value) }.copyAvailable()
}

/**
 * Binds [String] to `common/utf8`.
 */
public object StringCodec : SizedCodec<String>() {
    override val identity: String
        get() = "common/utf8"

    override suspend fun customEncode(value: String): Payload = value.encodeToByteArray()
    override suspend fun customDecode(payload: Payload): String = payload.decodeToString()
}

/**
 * Allows to create create codecs which write the payload size before serialized value.
 *
 * @param T the type of decoded and encoded object.
 */
public abstract class SizedCodec<T : Any> : Codec<T> {
    public final override suspend fun encode(value: T): Payload {
        val out = ByteChannel(true)
        val encoded = customEncode(value)
        out.writeInt(encoded.size)
        out.writePacket(ByteReadPacket(encoded))
        return out.copyAvailable()
    }

    /**
     * Custom serialization function.
     */
    protected abstract suspend fun customEncode(value: T): Payload

    public final override suspend fun decode(payload: Payload): Pair<T, Int> {
        val inp = ByteReadChannel(payload)
        val length = inp.readInt()
        val encoded = inp.copyAvailable()
        return customDecode(encoded.sliceArray(0 until length)) to Int.SIZE_BYTES + length
    }

    /**
     * Custom deserialization function.
     */
    protected abstract suspend fun customDecode(payload: Payload): T
}

/**
 * Binds [Pair] to `common/tuple<A,B>`.
 *
 * @property codecOfA The codec of [A].
 * @property codecOfB The codec of [B].
 */
public class PairCodec<A : Any, B : Any>(
    public val codecOfA: Codec<A>,
    public val codecOfB: Codec<B>,
) : Codec<Pair<A, B>> {
    override val identity: String
        get() = "common/pair<${codecOfA.identity},${codecOfB.identity}>"

    override suspend fun encode(value: Pair<A, B>): Payload {
        val out = ByteChannel(true)
        val (a, b) = value
        out.writeFully(codecOfA.encode(a))
        out.writeFully(codecOfB.encode(b))
        return out.copyAvailable()
    }

    override suspend fun decode(payload: Payload): Pair<Pair<A, B>, Int> {
        val (a, lengthOfA) = codecOfA.decode(payload)
        val (b, lengthOfB) = codecOfB.decode(payload.copyOfRange(lengthOfA, payload.size))
        return (a to b) to lengthOfA + lengthOfB
    }
}

/**
 * Binds [Triple] to `common/tuple<A,B,C>`.
 *
 * @property codecOfA The codec of [A].
 * @property codecOfB The codec of [B].
 * @property codecOfC The codec of [C].
 */
public class TripleCodec<A : Any, B : Any, C : Any>(
    public val codecOfA: Codec<A>,
    public val codecOfB: Codec<B>,
    public val codecOfC: Codec<C>,
) : Codec<Triple<A, B, C>> {
    override val identity: String
        get() = "common/triple<${codecOfA.identity},${codecOfB.identity},${codecOfC.identity}>"

    override suspend fun encode(value: Triple<A, B, C>): Payload {
        val out = ByteChannel(true)
        val (a, b, c) = value
        out.writeFully(codecOfA.encode(a))
        out.writeFully(codecOfB.encode(b))
        out.writeFully(codecOfC.encode(c))
        return out.copyAvailable()
    }

    override suspend fun decode(payload: Payload): Pair<Triple<A, B, C>, Int> {
        var fragment = payload
        val (a, lengthOfA) = codecOfA.decode(fragment)
        fragment = fragment.sliceArray(lengthOfA until fragment.size)
        val (b, lengthOfB) = codecOfB.decode(fragment)
        fragment = fragment.sliceArray(lengthOfB until fragment.size)
        val (c, lengthOfC) = codecOfC.decode(fragment)
        return Triple(a, b, c) to lengthOfA + lengthOfB + lengthOfC
    }
}

/**
 * Binds [List] of [Any] to `common/list<T>`.
 *
 * @property codec The codec of elements.
 */
public class ListCodec<T : Any>(public val codec: Codec<T>) : Codec<List<T>> {
    override val identity: String
        get() = "common/list<${codec.identity}>"

    override suspend fun decode(payload: Payload): Pair<List<T>, Int> {
        val ch = ByteReadChannel(payload)
        val count = ch.readInt()
        var fragment = ch.copyAvailable()
        val objects = mutableListOf<T>()
        var overallLength = Int.SIZE_BYTES

        repeat(count) {
            println(fragment.decodeToString())
            val (decoded, length) = codec.decode(fragment)
            objects += decoded
            fragment = fragment.sliceArray(length until fragment.size)
            overallLength += length
        }

        return objects to overallLength
    }

    override suspend fun encode(value: List<T>): Payload {
        val out = ByteChannel(true)
        out.writeInt(value.size)
        value.forEach { element -> out.writeFully(codec.encode(element)) }
        return out.copyAvailable()
    }
}

/**
 * Binds objects that can be serialized with [KSerializer] to `common/json`.
 *
 * @param T the type of decoded and encoded object.
 * @property serializerOfT [KSerializer] of [T].
 * @property format The [Json] instance.
 */
public class JsonCodec<T : Any> constructor(
    public val serializerOfT: KSerializer<T>,
    public val format: Json = Json,
) : Codec<T> {
    public constructor(classOfT: KClass<T>, format: Json = Json) : this(classOfT.serializer(), format)

    override val identity: String
        get() = "common/json"

    override suspend fun decode(payload: Payload): Pair<T, Int> {
        val (str, length) = StringCodec.decode(payload)
        return format.decodeFromString(serializerOfT, str) to length
    }

    override suspend fun encode(value: T): Payload = StringCodec.encode(format.encodeToString(serializerOfT, value))
}

/**
 * Constructs [JsonCodec].
 *
 * @param T the type of decoded and encoded object.
 * @param format the [Json] instance.
 */
public inline fun <reified T : Any> JsonCodec(format: Json = Json): JsonCodec<T> = JsonCodec(T::class, format)

/**
 * Binds objects that can be serialized with [KSerializer] to `common/cbor`.
 *
 * @param T the type of decoded and encoded object.
 * @property serializerOfT [KSerializer] of [T].
 * @property format The [Json] instance.
 */
public class CborCodec<T : Any> constructor(
    public val serializerOfT: KSerializer<T>,
    public val format: Cbor = Cbor,
) : SizedCodec<T>() {
    public constructor(classOfT: KClass<T>, format: Cbor = Cbor) : this(classOfT.serializer(), format)

    override val identity: String
        get() = "common/cbor"

    override suspend fun customDecode(payload: Payload): T = format.decodeFromByteArray(serializerOfT, payload)
    override suspend fun customEncode(value: T): Payload = format.encodeToByteArray(serializerOfT, value)
}

/**
 * Constructs [CborCodec].
 *
 * @param T the type of decoded and encoded object.
 * @property format The [Cbor] instance.
 */
public inline fun <reified T : Any> CborCodec(format: Cbor = Cbor): CborCodec<T> = CborCodec(T::class, format)
