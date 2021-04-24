package space.kscience.communicator.api

import io.ktor.utils.io.*
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readBytes

private suspend fun ByteReadChannel.copyAvailable(): ByteArray =
    ByteArray(availableForRead).also { readFully(it) }

/**
 * Binds [Int] to 4-byte [Payload].
 */
public object IntCoder : Coder<Int> {
    public override val identity: String
        get() = "Int"

    public override suspend fun decode(payload: Payload): Int = ByteReadChannel(payload).readInt()

    public override suspend fun encode(value: Int): Payload =
        ByteChannel(true).also { it.writeInt(value) }.copyAvailable()
}

/**
 * Binds [Long] to 8-byte [Payload].
 */
public object LongCoder : Coder<Long> {
    public override val identity: String
        get() = "Long"

    public override suspend fun decode(payload: Payload): Long = ByteReadChannel(payload).readLong()

    public override suspend fun encode(value: Long): Payload =
        ByteChannel(true).also { it.writeLong(value) }.copyAvailable()
}

/**
 * Binds [ULong] to 8-byte [Payload].
 */
public object ULongCoder : Coder<ULong> {
    public override val identity: String
        get() = "ULong"

    public override suspend fun decode(payload: Payload): ULong = ByteReadChannel(payload).readLong().toULong()

    public override suspend fun encode(value: ULong): Payload =
        ByteChannel(true).also { it.writeLong(value.toLong()) }.copyAvailable()
}

/**
 * Binds [Double] to 8-byte [Payload].
 */
public object DoubleCoder : Coder<Double> {
    public override val identity: String
        get() = "Double"

    public override suspend fun decode(payload: Payload): Double = ByteReadChannel(payload).readDouble()

    public override suspend fun encode(value: Double): Payload =
        ByteChannel(true).also { it.writeDouble(value) }.copyAvailable()
}

/**
 * Binds [String] to payload of array of size N and int N before it.
 */
public object StringCoder : Coder<String> {
    public override val identity: String
        get() = "String"

    public override suspend fun decode(payload: Payload): String {
        val inp = ByteReadChannel(payload)
        val len = inp.readInt()
        return inp.readPacket(len).readText()
    }

    public override suspend fun encode(value: String): Payload {
        val out = ByteChannel(true)
        out.writeInt(value.encodeToByteArray().size)
        out.writePacket(ByteReadPacket(value.encodeToByteArray()))
        return out.copyAvailable()
    }
}

/**
 * Allows to create create coders which write the payload size before serialized value.
 *
 * @param T the type of decoded and encoded object.
 */
public abstract class SizedCoder<T> : Coder<T> {
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
    public abstract suspend fun customEncode(value: T): Payload

    public final override suspend fun decode(payload: Payload): T {
        val inp = ByteReadChannel(payload)
        val length = inp.readInt()
        val encoded = inp.readPacket(length).readBytes()
        return customDecode(encoded)
    }

    /**
     * Custom deserialization function.
     */
    public abstract suspend fun customDecode(payload: Payload): T
}

/**
 * Binds [Pair] to payload of two sections coded by provided pair of coders.
 *
 * @property firstCoder The coder of [A].
 * @property secondCoder The coder of [B].
 */
public class PairCoder<A, B>(
    public val firstCoder: Coder<A>,
    public val secondCoder: Coder<B>,
) : Coder<Pair<A, B>> {
    public override val identity: String
        get() = "Pair<${firstCoder.identity}, ${secondCoder.identity}>"

    public override suspend fun encode(value: Pair<A, B>): Payload =
        firstCoder.encode(value.first) + secondCoder.encode(value.second)

    public override suspend fun decode(payload: Payload): Pair<A, B> {
        val v1 = firstCoder.decode(payload)
        val v2 = secondCoder.decode(payload.copyOfRange(firstCoder.encode(v1).size, payload.size))
        return Pair(v1, v2)
    }
}

/**
 * Binds [Triple] to payload of two sections coded by provided triple of coders.
 *
 * @property firstCoder The coder of [A].
 * @property secondCoder The coder of [B].
 * @property thirdCoder The coder of [C].
 */
public class TripleCoder<A, B, C>(
    public val firstCoder: Coder<A>,
    public val secondCoder: Coder<B>,
    public val thirdCoder: Coder<C>,
) : Coder<Triple<A, B, C>> {
    public override val identity: String
        get() = "Triple<${firstCoder.identity}, ${secondCoder.identity}, ${thirdCoder.identity}>"

    public override suspend fun encode(value: Triple<A, B, C>): Payload = firstCoder
        .encode(value.first)
        .plus(secondCoder.encode(value.second))
        .plus(thirdCoder.encode(value.third))

    public override suspend fun decode(payload: Payload): Triple<A, B, C> {
        var start = 0
        val v1 = firstCoder.decode(payload)
        start += firstCoder.encode(v1).size
        val v2 = secondCoder.decode(payload.copyOfRange(start, payload.size))
        start += secondCoder.encode(v2).size
        val v3 = thirdCoder.decode(payload.copyOfRange(start, payload.size))
        return Triple(v1, v2, v3)
    }
}
