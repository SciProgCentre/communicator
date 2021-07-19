package space.kscience.communicator.api

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CodecsTest {
    private suspend fun <T : Any> testEncodeAndDecode(codec: Codec<T>, vararg values: T) = values.forEach {
        assertEquals(it, codec.decode(codec.encode(it)).first)
    }

    @Test
    fun int() = runBlocking { testEncodeAndDecode(IntCodec, 0, 1, Int.MAX_VALUE, Int.MIN_VALUE, 42) }

    @Test
    fun long() = runBlocking { testEncodeAndDecode(LongCodec, 0L, 1L, Long.MAX_VALUE, Long.MIN_VALUE, 42L) }

    @Test
    fun ulong() = runBlocking { testEncodeAndDecode(ULongCodec, 0uL, 1uL, ULong.MAX_VALUE, 42uL) }

    @Test
    fun double() = runBlocking {
        testEncodeAndDecode(
            DoubleCodec,
            0.0,
            1.0,
            Double.MAX_VALUE,
            Double.MIN_VALUE,
            42.0,
            PI,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NaN,
        )
    }

    @Test
    fun string() = runBlocking {
        testEncodeAndDecode(StringCodec, "", "42", "bla".repeat(1000), "语言处理", "добрый вечер")
    }

    @Test
    fun pair() = runBlocking {
        testEncodeAndDecode(
            PairCodec(IntCodec, StringCodec),
            42 to "ku",
            21 to "yyy",
            Int.MAX_VALUE to "bla".repeat(100),
        )
    }

    @Test
    fun triple() = runBlocking {
        testEncodeAndDecode(
            TripleCodec(IntCodec, StringCodec, LongCodec),
            Triple(42, "ku", 323L),
            Triple(21, "yyy", 13L),
            Triple(Int.MAX_VALUE, "bla".repeat(100), 2302131233L),
        )
    }

    @Test
    fun sized() = runBlocking {
        data class SomeClass(val int: Int, val double: Double)

        val codec = object : SizedCodec<SomeClass>() {
            private val codec = PairCodec(IntCodec, DoubleCodec)

            override val identity: String
                get() = "SomeClass"

            override suspend fun customEncode(value: SomeClass): Payload = codec.encode(value.int to value.double)

            override suspend fun customDecode(payload: Payload): SomeClass {
                val (decoded, _) = codec.decode(payload)
                val (a, b) = decoded
                return SomeClass(a, b)
            }
        }

        testEncodeAndDecode(
            codec,
            SomeClass(42, 42.0),
            SomeClass(-2100000, Double.NaN),
            SomeClass(Int.MAX_VALUE, 21341234.0),
        )
    }

    @Test
    fun tuple() = runBlocking {
        testEncodeAndDecode(TupleCodec(listOf(IntCodec, LongCodec, StringCodec)), listOf(1, 2L, "123"))
    }

    @Test
    fun list() = runBlocking {
        testEncodeAndDecode(ListCodec(StringCodec), listOf("12341", "awerjhq", "9324148hrwst"))
    }

    @Serializable
    data class MyData(val a: Int, val b: String, val c: Set<Int>)

    @Test
    fun json() = runBlocking {
        testEncodeAndDecode(JsonCodec(), MyData(1, "2134oi5", setOf(1, 2, 3)))
    }

    @Test
    fun cbor() = runBlocking {
        testEncodeAndDecode(JsonCodec(), MyData(1, "2134oi5", setOf(1, 2, 3)))
    }
}
