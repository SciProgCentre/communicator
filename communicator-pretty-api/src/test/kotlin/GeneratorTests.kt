package space.kscience.communicator.prettyapi

import kotlinx.serialization.Serializable
import space.kscience.communicator.api.Codec
import space.kscience.communicator.api.IntCodec
import space.kscience.communicator.api.Payload
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GeneratorTests {
    @Serializable
    data class Structure(val a: Int, val b: Set<Int>)

    interface Api1 {
        suspend fun boo(j: Structure): Int
    }

    @Test
    fun json() = runTest {
        functionServer<Api1>(object : Api1 {
            override suspend fun boo(j: Structure): Int = j.a * j.b.sum()
        })

        val api = functionClient<Api1>()
        assertEquals(6, api.boo(Structure(2, setOf((3)))))
    }

    @Test
    fun cbor() = runTest {
        serializableFormat = CBOR

        functionServer<Api1>(object : Api1 {
            override suspend fun boo(j: Structure): Int = j.a * j.b.sum()
        })

        val api = functionClient<Api1>()
        assertEquals(6, api.boo(Structure(2, setOf((3)))))
    }


    data class X(val i: Int)

    interface Api2 {
        suspend fun boo(j: X): Int
    }

    @Test
    fun customCodec() = runTest {
        registerCodec(object : Codec<X> {
            override val identity: String
                get() = "custom/x"

            override suspend fun decode(payload: Payload): Pair<X, Int> {
                val (i, length) = IntCodec.decode(payload)
                return X(i) to length
            }

            override suspend fun encode(value: X): Payload = IntCodec.encode(value.i)
        })

        functionServer<Api2>(object : Api2 {
            override suspend fun boo(j: X): Int = j.i
        })

        val api = functionClient<Api2>()
        assertEquals(61231234, api.boo(X(61231234)))
    }


    interface Api3 {
        suspend fun boo(a: String, b: Int, c: Double, d: Float, e: Long, f: ULong): String
    }

    @Test
    fun manyParameters() = runTest {
        functionServer<Api3>(object : Api3 {
            override suspend fun boo(a: String, b: Int, c: Double, d: Float, e: Long, f: ULong): String {
                return a + b + c + d + e + f
            }
        })

        val api = functionClient<Api3>()
        assertEquals("", api.boo("1", 2, 3.0, 4f, 5L, 6uL))
    }
}
