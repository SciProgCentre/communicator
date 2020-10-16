package kscience.communicator.api

import kotlinx.io.*

/**
 * Binds [Int] to 4-byte [Payload].
 */
object IntCoder : Coder<Int> {
    override val identity: String
        get() = "Int"

    override fun decode(payload: Payload): Int = ByteArrayInput(payload).readInt()
    override fun encode(value: Int): Payload = ByteArrayOutput(Int.SIZE_BYTES).also { it.writeInt(value) }.toByteArray()
    override fun toString(): String = "intCoder"
}

/**
 * Binds [Long] to 8-byte [Payload].
 */
object LongCoder : Coder<Long> {
    override val identity: String
        get() = "Long"

    override fun decode(payload: Payload): Long = ByteArrayInput(payload).readLong()

    override fun encode(value: Long): Payload =
        ByteArrayOutput(Long.SIZE_BYTES).also { it.writeLong(value) }.toByteArray()

    override fun toString(): String = "longCoder"
}

/**
 * Binds [ULong] to 8-byte [Payload].
 */
object ULongCoder : Coder<ULong> {
    override val identity: String
        get() = "ULong"

    override fun decode(payload: Payload): ULong = ByteArrayInput(payload).readULong()

    override fun encode(value: ULong): Payload =
        ByteArrayOutput(ULong.SIZE_BYTES).also { it.writeULong(value) }.toByteArray()

    override fun toString(): String = "ulongCoder"
}

/**
 * Binds [Float] to 4-byte [Payload].
 */
object FloatCoder : Coder<Float> {
    override val identity: String
        get() = "Float"

    override fun decode(payload: Payload): Float = ByteArrayInput(payload).readFloat()

    override fun encode(value: Float): Payload =
        ByteArrayOutput(4).also { it.writeFloat(value) }.toByteArray()

    override fun toString(): String = "floatCoder"
}

/**
 * Binds [Double] to 8-byte [Payload].
 */
object DoubleCoder : Coder<Double> {
    override val identity: String
        get() = "Double"

    override fun decode(payload: Payload): Double = ByteArrayInput(payload).readDouble()

    override fun encode(value: Double): Payload =
        ByteArrayOutput(8).also { it.writeDouble(value) }.toByteArray()

    override fun toString(): String = "doubleCoder"
}

/**
 * Binds [String] to payload of array of size N and int N before it.
 */
object StringCoder : Coder<String> {
    override val identity: String
        get() = "String"

    override fun decode(payload: Payload): String {
        val inp = ByteArrayInput(payload)
        val len = inp.readInt()
        return inp.readByteArray(len).decodeToString()
    }

    override fun encode(value: String): Payload {
        val codedValue = value.encodeToByteArray()
        val out = ByteArrayOutput()
        out.writeInt(codedValue.size)
        out.writeByteArray(codedValue)
        return out.toByteArray()
    }

    override fun toString(): String = "stringCoder"
}

class RemoteFunctionCoder<T, R>(val functionSpec: FunctionSpec<T, R>) : Coder<RemoteFunction<T, R>> {

    override val identity: String
        get() = "RemoteFunction<${functionSpec.argumentCoder.identity}, ${functionSpec.resultCoder.identity}>"

    override fun encode(value: RemoteFunction<T, R>): Payload {
        val out = ByteArrayOutput()
        out.writeInt(value.name.length)
        out.writeByteArray(value.name.encodeToByteArray())
        out.writeInt(value.endpoint.protocol.length)
        out.writeByteArray(value.endpoint.protocol.encodeToByteArray())
        out.writeInt(value.endpoint.address.length)
        out.writeByteArray(value.endpoint.address.encodeToByteArray())
        return out.toByteArray()
    }

    override fun decode(payload: Payload): RemoteFunction<T, R> {
        val inp = ByteArrayInput(payload)

        val nameLength = inp.readInt()
        val name = inp.readByteArray(nameLength).decodeToString()
        val protocolLength = inp.readInt()
        val protocol = inp.readByteArray(protocolLength).decodeToString()
        val addressLength = inp.readInt()
        val address = inp.readByteArray(addressLength).decodeToString()

        return RemoteFunction(name, Endpoint(protocol, address), functionSpec)
    }

    override fun toString(): String = "functionCoder"

}

/**
 * Binds [List] to payload of sequence of sub-payloads for [T].
 *
 * @param T the type of items contained in list.
 * @property elementCoder The coder of [T].
 */
class ListCoder<T>(val elementCoder: Coder<T>) : Coder<List<T>> {
    override val identity: String
        get() = "List<${elementCoder.identity}>"

    override fun encode(value: List<T>): Payload {
        val out = ByteArrayOutput()
        out.writeInt(value.size)
        value.asSequence().map(elementCoder::encode).forEach { out.writeByteArray(it) }
        return out.toByteArray()
    }

    override fun decode(payload: Payload): List<T> {
        val res = mutableListOf<T>()
        val inp = ByteArrayInput(payload)
        val length = inp.readInt()
        repeat(length) {
            val elem = elementCoder.decode(inp.preview { inp.readByteArray() })
            res.add(elem)
            inp.readByteArray(elementCoder.encode(elem).size)
        }

        return res
    }

    override fun toString(): String = "arrayCoder"
}

class MapCoder<K, V>(val keyCoder: Coder<K>, val valueCoder: Coder<V>) : Coder<Map<K, V>> {

    private val actualCoder = ListCoder(PairCoder(keyCoder, valueCoder))

    override val identity: String
        get() = "Map<${keyCoder.identity}, ${valueCoder.identity}>"

    override fun encode(value: Map<K, V>): Payload = actualCoder.encode(value.toList())

    override fun decode(payload: Payload): Map<K, V> = actualCoder.decode(payload).toMap()

    override fun toString(): String = "arrayCoder"
}

abstract class CustomCoder<T> : Coder<T> {

    override fun encode(value: T): Payload {
        val out = ByteArrayOutput()
        val encoded = customEncode(value)
        out.writeInt(encoded.size)
        out.writeByteArray(encoded)
        return out.toByteArray()
    }

    abstract fun customEncode(value: T): Payload

    override fun decode(payload: Payload): T {
        val inp = ByteArrayInput(payload)
        val length = inp.readInt()
        val encoded = inp.readByteArray(length)
        return customDecode(encoded)
    }

    abstract fun customDecode(payload: Payload): T

}

class CompositeObjectField<T, F>(val getter: (T) -> F, val coder: Coder<F>)

class CompositeCoder<T>(val composer: (List<*>) -> T, val fields: List<CompositeObjectField<T, *>>) : Coder<T> {

    override val identity: String
        get() = "Object<${fields.joinToString(prefix = "", postfix = "") { it.coder.identity }}>"

    override fun encode(value: T): Payload {
        val out = ByteArrayOutput()
        fields.asSequence().map {
            @Suppress("UNCHECKED_CAST")
            (it.coder as Coder<Any?>).encode(it.getter(value))
        }.forEach { out.writeByteArray(it) }
        return out.toByteArray()
    }

    override fun decode(payload: Payload): T {
        val res = mutableListOf<Any?>()
        val inp = ByteArrayInput(payload)
        fields.forEach {
            val elem = it.coder.decode(inp.preview { inp.readByteArray() })
            res.add(elem)
            @Suppress("UNCHECKED_CAST")
            inp.readByteArray((it.coder as Coder<Any?>).encode(elem).size)
        }
        return composer(res)
    }

    override fun toString(): String = "arrayCoder"
}

/**
 * Binds [Pair] to payload of two sections coded by provided pair of coders.
 *
 * @property firstCoder The coder of [A].
 * @property secondCoder The coder of [B].
 */
class PairCoder<A, B>(
    val firstCoder: Coder<A>,
    val secondCoder: Coder<B>
) : Coder<Pair<A, B>> {
    override val identity: String
        get() = "Pair<${firstCoder.identity}, ${secondCoder.identity}>"

    override fun encode(value: Pair<A, B>): Payload = firstCoder.encode(value.first) +
            secondCoder.encode(value.second)

    override fun decode(payload: Payload): Pair<A, B> {
        var start = 0
        val v1 = firstCoder.decode(payload)
        start += firstCoder.encode(v1).size
        val v2 = secondCoder.decode(payload)
        start += secondCoder.encode(v2).size
        return Pair(v1, v2)
    }

    override fun toString(): String = TODO("Not yet implemented")
}

/**
 * Binds [Triple] to payload of two sections coded by provided triple of coders.
 *
 * @property firstCoder The coder of [A].
 * @property secondCoder The coder of [B].
 * @property thirdCoder The coder of [C].
 */
class TripleCoder<A, B, C>(
    val firstCoder: Coder<A>,
    val secondCoder: Coder<B>,
    val thirdCoder: Coder<C>
) : Coder<Triple<A, B, C>> {
    override val identity: String
        get() = "Triple<${firstCoder.identity}, ${secondCoder.identity}, ${thirdCoder.identity}>"

    override fun encode(value: Triple<A, B, C>): Payload = firstCoder
        .encode(value.first)
        .plus(secondCoder.encode(value.second))
        .plus(thirdCoder.encode(value.third))

    override fun decode(payload: Payload): Triple<A, B, C> {
        var start = 0
        val v1 = firstCoder.decode(payload)
        start += firstCoder.encode(v1).size
        val v2 = secondCoder.decode(payload)
        start += secondCoder.encode(v2).size
        val v3 = thirdCoder.decode(payload)
        start += thirdCoder.encode(v3).size
        return Triple(v1, v2, v3)
    }

    override fun toString(): String = TODO("Not yet implemented")
}
