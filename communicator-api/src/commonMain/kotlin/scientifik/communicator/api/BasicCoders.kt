package scientifik.communicator.api


fun <T> Coder<T>.logging(): Coder<T> = object : Coder<T> {
    override fun encode(value: T): Payload {
        val result = this@logging.encode(value)
        println("Encoded $value to ${result.contentToString()}")
        return result
    }

    override fun decode(bin: Payload): T {
        val result = this@logging.decode(bin)
        println("Decoded ${bin.contentToString()} to $result")
        return result
    }

    override val identity: ByteArray
        get() = this@logging.identity

    override fun toString(): String = this@logging.toString()

}

class IntCoder: Coder<Int> {
    // Taken from https://medium.com/@bananaumai/kotlin-convert-integers-into-bytearray-ca7a2bd9718a
    private fun Int.toByteArray(isBigEndian: Boolean = true): ByteArray {
        var bytes = byteArrayOf()

        var n = this

        if (n == 0 || n == -1) {
            bytes += n.toByte()
        } else if (n > 0) {
            while (n != 0) {
                val b = n.and(0xFF).toByte()

                bytes += b

                n = n.shr(Byte.SIZE_BITS)
            }
        } else {
            while (n != -1) {
                val b = n.and(0xFF).toByte()

                bytes += b

                n = n.shr(Byte.SIZE_BITS)
            }
        }

        val padding = if (n < 0) {
            0xFF.toByte()
        } else {
            0x00.toByte()
        }
        var paddings = byteArrayOf()
        repeat(Int.SIZE_BYTES - bytes.count()) {
            paddings += padding
        }

        return if (isBigEndian) {
            paddings + bytes.reversedArray()
        } else {
            paddings + bytes
        }
    }

    // Taken from https://stackoverflow.com/questions/56872782/convert-byte-array-to-int-odd-result-java-and-kotlin/56873062
    private fun intFromByteArray(bytes: ByteArray): Int {
        var result = 0
        var shift = 0
        for (byte in bytes) {
            result = result or (byte.toInt() shl shift)
            shift += 8
        }
        return result
    }

    override fun decode(bin: Payload): Int {
        return intFromByteArray(bin)
    }

    override fun encode(value: Int): Payload {
        return value.toByteArray()
    }

    override fun toString(): String {
        return "intCoder"
    }

    override val identity: ByteArray
        get() = TODO("Not yet implemented")

}

class StringCoder: Coder<String> {

    override fun decode(bin: Payload): String {
        val len = IntCoder().decode(bin.slice(0..3).toByteArray())
        return bin.slice(4 until len + 4).toByteArray().decodeToString()
    }

    override fun encode(value: String): Payload {
        val codedLen = IntCoder().encode(value.length)
        val codedValue = value.encodeToByteArray()
        return codedLen + codedValue
    }

    override fun toString(): String {
        return "stringCoder"
    }

    override val identity: ByteArray
        get() = TODO("Not yet implemented")

}

class IterableCoder<T>(val elementCoder: Coder<T>): Coder<Iterable<T>> {
    override fun encode(value: Iterable<T>): Payload {
        return value.fold(byteArrayOf()) { acc, elem ->
            acc + elementCoder.encode(elem)
        }
    }

    override fun decode(bin: Payload): Iterable<T> {
        val res = mutableListOf<T>()
        var start = 0
        while (start < bin.size) {
            val elem = elementCoder.decode(bin.slice(start until bin.size).toByteArray())
            res.add(elem)
            start += elementCoder.encode(elem).size
        }
        return res
    }

    override fun toString(): String {
        TODO("Not yet implemented")
    }

    override val identity: ByteArray
        get() = TODO("Not yet implemented")
}

class PairCoder<T1, T2>(
        val component1Coder: Coder<T1>,
        val component2Coder: Coder<T2>): Coder<Pair<T1, T2>> {
    override fun encode(value: Pair<T1, T2>): Payload {
        return component1Coder.encode(value.first) +
                component2Coder.encode(value.second)
    }

    override fun decode(bin: Payload): Pair<T1, T2> {
        var start = 0
        val v1 = component1Coder.decode(bin)
        start += component1Coder.encode(v1).size

        val v2 = component2Coder.decode(bin)
        start += component2Coder.encode(v2).size

        return Pair(v1, v2)
    }

    override fun toString(): String {
        TODO("Not yet implemented")
    }

    override val identity: ByteArray
        get() = TODO("Not yet implemented")
}

class TripleCoder<T1, T2, T3>(
        val component1Coder: Coder<T1>,
        val component2Coder: Coder<T2>,
        val component3Coder: Coder<T3>): Coder<Triple<T1, T2, T3>> {
    override fun encode(value: Triple<T1, T2, T3>): Payload {
        return component1Coder.encode(value.first) +
                component2Coder.encode(value.second) +
                component3Coder.encode(value.third)
    }

    override fun decode(bin: Payload): Triple<T1, T2, T3> {
        var start = 0
        val v1 = component1Coder.decode(bin)
        start += component1Coder.encode(v1).size

        val v2 = component2Coder.decode(bin)
        start += component2Coder.encode(v2).size

        val v3 = component3Coder.decode(bin)
        start += component3Coder.encode(v3).size

        return Triple(v1, v2, v3)
    }

    override fun toString(): String {
        TODO("Not yet implemented")
    }

    override val identity: ByteArray
        get() = TODO("Not yet implemented")

}
