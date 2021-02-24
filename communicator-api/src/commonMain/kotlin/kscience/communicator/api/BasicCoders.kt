package kscience.communicator.api

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.readPacket
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map

/**
 * Binds [Int] to 4-byte [Payload].
 */
public object IntCoder : Coder<Int> {
    public override val identity: String
        get() = "Int"

    public override suspend fun decode(payload: Payload): Int = ByteReadChannel(payload).readInt()

    public override suspend fun encode(value: Int): Payload =
        ByteChannel(true).also { it.writeInt(value) }.readPacket(Int.SIZE_BYTES).readBytes()
}

/**
 * Binds [Long] to 8-byte [Payload].
 */
public object LongCoder : Coder<Long> {
    public override val identity: String
        get() = "Long"

    public override suspend fun decode(payload: Payload): Long = ByteReadChannel(payload).readLong()

    public override suspend fun encode(value: Long): Payload =
        ByteChannel(true).also { it.writeLong(value) }.readPacket(Long.SIZE_BYTES).readBytes()
}

/**
 * Binds [ULong] to 8-byte [Payload].
 */
public object ULongCoder : Coder<ULong> {
    public override val identity: String
        get() = "ULong"

    public override suspend fun decode(payload: Payload): ULong = ByteReadChannel(payload).readLong().toULong()

    public override suspend fun encode(value: ULong): Payload =
        ByteChannel(true).also { it.writeLong(value.toLong()) }.readPacket(Long.SIZE_BYTES).readBytes()
}
//
///**
// * Binds [Float] to 4-byte [Payload].
// */
//public object FloatCoder : Coder<Float> {
//    public override val identity: String
//        get() = "Float"
//
//    public override suspend fun decode(payload: Payload): Float = ByteReadChannel(payload).readFloat()
//
//    public override suspend fun encode(value: Float): Payload =
//        ByteChannel(true).also { it.writeFloat(value) }.readPacket().readBytes()
//}
//
///**
// * Binds [Double] to 8-byte [Payload].
// */
//public object DoubleCoder : Coder<Double> {
//    public override val identity: String
//        get() = "Double"
//
//    public override suspend fun decode(payload: Payload): Double = ByteReadChannel(payload).readDouble()
//
//    public override suspend fun encode(value: Double): Payload =
//        ByteChannel(true).also { it.writeDouble(value) }.readRemaining().readBytes()
//}
//
///**
// * Binds [String] to payload of array of size N and int N before it.
// */
//public object StringCoder : Coder<String> {
//    public override val identity: String
//        get() = "String"
//
//    public override suspend fun decode(payload: Payload): String {
//        val inp = ByteReadChannel(payload)
//        val len = inp.readInt()
//        return inp.readRemaining(len.toLong()).readText()
//    }
//
//    public override suspend fun encode(value: String): Payload {
//        val out = ByteChannel(true)
//        out.writeInt(value.encodeToByteArray().size)
//        out.writePacket(ByteReadPacket(value.encodeToByteArray()))
//        return out.readRemaining().readBytes()
//    }
//}
//
//public class RemoteFunctionCoder<T, R>(public val functionSpec: FunctionSpec<T, R>) : Coder<RemoteFunction<T, R>> {
//    public override val identity: String
//        get() = "RemoteFunction<${functionSpec.argumentCoder.identity}, ${functionSpec.resultCoder.identity}>"
//
//    public override suspend fun encode(value: RemoteFunction<T, R>): Payload {
//        val out = ByteChannel(true)
//        out.writeInt(value.name.length)
//        out.writePacket(ByteReadPacket(value.name.encodeToByteArray()))
//        out.writeInt(value.endpoint.protocol.length)
//        out.writePacket(ByteReadPacket(value.endpoint.protocol.encodeToByteArray()))
//        out.writeInt(value.endpoint.address.length)
//        out.writePacket(ByteReadPacket(value.endpoint.address.encodeToByteArray()))
//        return out.readRemaining().readBytes()
//    }
//
//    public override suspend fun decode(payload: Payload): RemoteFunction<T, R> {
//        val inp = ByteReadChannel(payload)
//        val nameLength = inp.readInt()
//        val name = inp.readPacket(nameLength).readBytes().decodeToString()
//        val protocolLength = inp.readInt()
//        val protocol = inp.readPacket(protocolLength).readBytes().decodeToString()
//        val addressLength = inp.readInt()
//        val address = inp.readPacket(addressLength).readBytes().decodeToString()
//        return RemoteFunction(name, Endpoint(protocol, address), functionSpec)
//    }
//}

///**
// * Binds [List] to payload of sequence of sub-payloads for [T].
// *
// * @param T the type of items contained in list.
// * @property elementCoder The coder of [T].
// */
//public class ListCoder<T>(public val elementCoder: Coder<T>) : Coder<List<T>> {
//    override val identity: String
//        get() = "List<${elementCoder.identity}>"
//
//    @OptIn(InternalCoroutinesApi::class)
//    override suspend fun encode(value: List<T>): Payload {
//        val out = ByteChannel(true)
//        out.writeInt(value.size)
//        value.asFlow().map { elementCoder.encode(it) }.collect { out.writeByteArray(it) }
//        return out.toByteArray()
//    }
//
//    override suspend fun decode(payload: Payload): List<T> {
//        val res = mutableListOf<T>()
//        val inp = ByteReadChannel(payload)
//        val length = inp.readInt()
//
//        repeat(length) {
//            val elem = elementCoder.decode(inp.read { inp.readByteArray() })
//            res.add(elem)
//            inp.readPacket(elementCoder.encode(elem).size)
//        }
//
//        return res
//    }
//}

//public class MapCoder<K, V>(public val keyCoder: Coder<K>, public val valueCoder: Coder<V>) : Coder<Map<K, V>> {
//    private val actualCoder = ListCoder(PairCoder(keyCoder, valueCoder))
//
//    override val identity: String
//        get() = "Map<${keyCoder.identity}, ${valueCoder.identity}>"
//
//    override suspend fun encode(value: Map<K, V>): Payload = actualCoder.encode(value.toList())
//    override suspend fun decode(payload: Payload): Map<K, V> = actualCoder.decode(payload).toMap()
//}

//public abstract class CustomCoder<T> : Coder<T> {
//    public override suspend fun encode(value: T): Payload {
//        val out = ByteChannel(true)
//        val encoded = customEncode(value)
//        out.writeInt(encoded.size)
//        out.writePacket(ByteReadPacket(encoded))
//        return out.readRemaining().readBytes()
//    }
//
//    public abstract fun customEncode(value: T): Payload
//
//    public override suspend fun decode(payload: Payload): T {
//        val inp = ByteReadChannel(payload)
//        val length = inp.readInt()
//        val encoded = inp.readPacket(length).readBytes()
//        return customDecode(encoded)
//    }
//
//    public abstract fun customDecode(payload: Payload): T
//}
//
//public class CompositeObjectField<T, F>(public val getter: (T) -> F, public val coder: Coder<F>)

//public class CompositeCoder<T>(
//    public val composer: (List<*>) -> T,
//    public val fields: List<CompositeObjectField<T, *>>,
//) : Coder<T> {
//    public override val identity: String
//        get() = "Object<${fields.joinToString(prefix = "", postfix = "") { it.coder.identity }}>"
//
//    @Suppress("UNCHECKED_CAST")
//    public override suspend fun encode(value: T): Payload {
//        val out = ByteChannel(true)
//
//        fields.asFlow()
//            .map { (it.coder as Coder<Any?>).encode(it.getter(value)) }
//            .collect { out.writePacket(ByteReadPacket(it)) }
//
//        return out.readRemaining().readBytes()
//    }
//
//    public override suspend fun decode(payload: Payload): T {
//        val res = mutableListOf<Any?>()
//        val inp = ByteReadChannel(payload)
//        fields.forEach {
//            val elem = it.coder.decode(inp.preview { inp.readByteArray() })
//            res.add(elem)
//            @Suppress("UNCHECKED_CAST")
//            inp.readByteArray((it.coder as Coder<Any?>).encode(elem).size)
//        }
//        return composer(res)
//    }
//}
//
///**
// * Binds [Pair] to payload of two sections coded by provided pair of coders.
// *
// * @property firstCoder The coder of [A].
// * @property secondCoder The coder of [B].
// */
//public class PairCoder<A, B>(
//    public val firstCoder: Coder<A>,
//    public val secondCoder: Coder<B>,
//) : Coder<Pair<A, B>> {
//    public override val identity: String
//        get() = "Pair<${firstCoder.identity}, ${secondCoder.identity}>"
//
//    public override suspend fun encode(value: Pair<A, B>): Payload = firstCoder.encode(value.first) +
//            secondCoder.encode(value.second)
//
//    public override suspend fun decode(payload: Payload): Pair<A, B> {
//        var start = 0
//        val v1 = firstCoder.decode(payload)
//        start += firstCoder.encode(v1).size
//        val v2 = secondCoder.decode(payload)
//        start += secondCoder.encode(v2).size
//        return Pair(v1, v2)
//    }
//}
//
///**
// * Binds [Triple] to payload of two sections coded by provided triple of coders.
// *
// * @property firstCoder The coder of [A].
// * @property secondCoder The coder of [B].
// * @property thirdCoder The coder of [C].
// */
//public class TripleCoder<A, B, C>(
//    public val firstCoder: Coder<A>,
//    public val secondCoder: Coder<B>,
//    public val thirdCoder: Coder<C>,
//) : Coder<Triple<A, B, C>> {
//    public override val identity: String
//        get() = "Triple<${firstCoder.identity}, ${secondCoder.identity}, ${thirdCoder.identity}>"
//
//    public override suspend fun encode(value: Triple<A, B, C>): Payload = firstCoder
//        .encode(value.first)
//        .plus(secondCoder.encode(value.second))
//        .plus(thirdCoder.encode(value.third))
//
//    public override suspend fun decode(payload: Payload): Triple<A, B, C> {
//        var start = 0
//        val v1 = firstCoder.decode(payload)
//        start += firstCoder.encode(v1).size
//        val v2 = secondCoder.decode(payload)
//        start += secondCoder.encode(v2).size
//        val v3 = thirdCoder.decode(payload)
//        start += thirdCoder.encode(v3).size
//        return Triple(v1, v2, v3)
//    }
//}
