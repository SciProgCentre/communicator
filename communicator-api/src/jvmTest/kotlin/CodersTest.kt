package space.kscience.communicator.api

import kotlinx.coroutines.runBlocking
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CodersTest {
    private suspend fun <T> testEncodeAndDecode(coder: Coder<T>, vararg values: T) = values.forEach {
        assertEquals(it, coder.decode(coder.encode(it)))
    }

    @Test
    fun int() = runBlocking { testEncodeAndDecode(IntCoder, 0, 1, Int.MAX_VALUE, Int.MIN_VALUE, 42) }

    @Test
    fun long() = runBlocking { testEncodeAndDecode(LongCoder, 0L, 1L, Long.MAX_VALUE, Long.MIN_VALUE, 42L) }

    @Test
    fun ulong() = runBlocking { testEncodeAndDecode(ULongCoder, 0uL, 1uL, ULong.MAX_VALUE, 42uL) }

    @Test
    fun double() = runBlocking {
        testEncodeAndDecode(
            DoubleCoder,
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
        testEncodeAndDecode(StringCoder, "", "42", "bla".repeat(1000), "语言处理", "добрый вечер")
    }

    @Test
    fun pair() = runBlocking {
        testEncodeAndDecode(
            PairCoder(IntCoder, StringCoder),
            42 to "ku",
            21 to "yyy",
            Int.MAX_VALUE to "bla".repeat(100),
        )
    }

    @Test
    fun triple() = runBlocking {
        testEncodeAndDecode(
            TripleCoder(IntCoder, StringCoder, LongCoder),
            Triple(42, "ku", 323L),
            Triple(21, "yyy", 13L),
            Triple(Int.MAX_VALUE, "bla".repeat(100), 2302131233L),
        )
    }

    @Test
    fun custom() = runBlocking {
        data class SomeClass(val int: Int, val double: Double)

        val coder = object : SizedCoder<SomeClass>() {
            private val coder = PairCoder(IntCoder, DoubleCoder)

            override val identity: String
                get() = "SomeClass"

            override suspend fun customEncode(value: SomeClass): Payload = coder.encode(value.int to value.double)

            override suspend fun customDecode(payload: Payload): SomeClass {
                val (a, b) = coder.decode(payload)
                return SomeClass(a, b)
            }
        }

        testEncodeAndDecode(
            coder,
            SomeClass(42, 42.0),
            SomeClass(-2100000, Double.NaN),
            SomeClass(Int.MAX_VALUE, 21341234.0),
        )
    }
}
