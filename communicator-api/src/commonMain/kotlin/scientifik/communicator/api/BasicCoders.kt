package scientifik.communicator.api

import kotlinx.io.*

object IntCoder : Coder<Int> {
    override val identity: ByteArray
        get() = TODO("Not yet implemented")

    override fun decode(bin: Payload): Int = ByteArrayInput(bin).readInt()
    override fun encode(value: Int): Payload = ByteArrayOutput(Int.SIZE_BYTES).also { it.writeInt(value) }.toByteArray()
    override fun toString(): String = "intCoder"
}

object LongCoder : Coder<Long> {
    override val identity: ByteArray
        get() = TODO("Not yet implemented")

    override fun decode(bin: Payload): Long = ByteArrayInput(bin).readLong()

    override fun encode(value: Long): Payload =
        ByteArrayOutput(Long.SIZE_BYTES).also { it.writeLong(value) }.toByteArray()

    override fun toString(): String = "longCoder"
}

object ULongCoder : Coder<ULong> {
    override val identity: ByteArray
        get() = TODO("Not yet implemented")

    override fun decode(bin: Payload): ULong = ByteArrayInput(bin).readULong()

    override fun encode(value: ULong): Payload =
        ByteArrayOutput(ULong.SIZE_BYTES).also { it.writeULong(value) }.toByteArray()

    override fun toString(): String = "ulongCoder"
}

object FloatCoder : Coder<Float> {
    override val identity: ByteArray
        get() = TODO("Not yet implemented")

    override fun decode(bin: Payload): Float = ByteArrayInput(bin).readFloat()

    override fun encode(value: Float): Payload =
        ByteArrayOutput(4).also { it.writeFloat(value) }.toByteArray()

    override fun toString(): String = "floatCoder"
}

object DoubleCoder : Coder<Double> {
    override val identity: ByteArray
        get() = TODO("Not yet implemented")

    override fun decode(bin: Payload): Double = ByteArrayInput(bin).readDouble()

    override fun encode(value: Double): Payload =
        ByteArrayOutput(8).also { it.writeDouble(value) }.toByteArray()

    override fun toString(): String = "doubleCoder"
}

object StringCoder : Coder<String> {
    override val identity: ByteArray
        get() = TODO("Not yet implemented")

    override fun decode(bin: Payload): String {
        val len = IntCoder.decode(bin.slice(0..3).toByteArray())
        return bin.slice(4 until len + 4).toByteArray().decodeToString()
    }

    override fun encode(value: String): Payload {
        val codedLen = IntCoder.encode(value.length)
        val codedValue = value.encodeToByteArray()
        return codedLen + codedValue
    }

    override fun toString(): String = "stringCoder"
}

class ListCoder<T>(val elementCoder: Coder<T>) : Coder<List<T>> {
    override val identity: ByteArray
        get() = TODO("Not yet implemented")

    override fun encode(value: List<T>): Payload {
        val out = ByteArrayOutput(0)
        value.asSequence().map(elementCoder::encode).forEach { out.writeByteArray(it) }
        return out.toByteArray()
    }

    override fun decode(bin: Payload): List<T> {
        val res = mutableListOf<T>()
        val inp = ByteArrayInput(bin)

        while (!inp.exhausted()) {
            val elem = elementCoder.decode(inp.preview { inp.readByteArray() })
            res.add(elem)
            inp.readByteArray(elementCoder.encode(elem).size)
        }

        return res
    }

    override fun toString(): String = "arrayCoder"
}

class PairCoder<A, B>(
    val firstCoder: Coder<A>,
    val secondCoder: Coder<B>
) : Coder<Pair<A, B>> {
    override val identity: ByteArray
        get() = TODO("Not yet implemented")

    override fun encode(value: Pair<A, B>): Payload = firstCoder.encode(value.first) +
            secondCoder.encode(value.second)

    override fun decode(bin: Payload): Pair<A, B> {
        var start = 0
        val v1 = firstCoder.decode(bin)
        start += firstCoder.encode(v1).size
        val v2 = secondCoder.decode(bin)
        start += secondCoder.encode(v2).size
        return Pair(v1, v2)
    }

    override fun toString(): String = TODO("Not yet implemented")
}

class TripleCoder<A, B, C>(
    val firstCoder: Coder<A>,
    val secondCoder: Coder<B>,
    val thirdCoder: Coder<C>
) : Coder<Triple<A, B, C>> {
    override val identity: ByteArray
        get() = TODO("Not yet implemented")

    override fun encode(value: Triple<A, B, C>): Payload = firstCoder
        .encode(value.first)
        .plus(secondCoder.encode(value.second))
        .plus(thirdCoder.encode(value.third))

    override fun decode(bin: Payload): Triple<A, B, C> {
        var start = 0
        val v1 = firstCoder.decode(bin)
        start += firstCoder.encode(v1).size
        val v2 = secondCoder.decode(bin)
        start += secondCoder.encode(v2).size
        val v3 = thirdCoder.decode(bin)
        start += thirdCoder.encode(v3).size
        return Triple(v1, v2, v3)
    }

    override fun toString(): String = TODO("Not yet implemented")
}
