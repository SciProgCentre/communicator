package space.kscience.communicator.prettyapi

import kotlinx.serialization.Serializable
import space.kscience.communicator.api.Codec
import space.kscience.communicator.api.IntCodec
import space.kscience.communicator.api.Payload
import space.kscience.communicator.api.StringCodec
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
        assertEquals("123.04.056", api.boo("1", 2, 3.0, 4f, 5L, 6uL))
    }


    interface Api4 {
        suspend fun boo(a: Pair<Int, Int>, b: Triple<Int, Int, Int>): String
    }

    @Test
    fun subtuples() = runTest {
        functionServer<Api4>(object : Api4 {
            override suspend fun boo(a: Pair<Int, Int>, b: Triple<Int, Int, Int>): String {
                val (a1, a2) = a
                val (b1, b2, b3) = b
                return ((a1 + a2) * (b1 + b2 + b3)).toString()
            }
        })

        val api = functionClient<Api4>()
        assertEquals("180", api.boo(1 to 2, Triple(10, 20, 30)))
    }


    interface Api5 {
        suspend fun boo(a: ULong): ULong
    }

//    TODO uncomment when KT-47973 is fixed
//    @Test
//    fun inlineClassOverPrimitive() = runTest {
//        functionServer<Api5>(object : Api5 {
//            override suspend fun boo(a: ULong): ULong = a
//        })
//
//        val api = functionClient<Api5>()
//        assertEquals(545uL, api.boo(545uL))
//    }


    @JvmInline
    value class Box(val obj: String)

    interface Api6 {
        suspend fun boo(a: Box): Box
    }

    @Test
    fun inlineClassOverObject() = runTest {
        registerCodec(object : Codec<Box> {
            override val identity: String
                get() = "custom/box"

            override suspend fun decode(payload: Payload): Pair<Box, Int> {
                val (s, length) = StringCodec.decode(payload)
                return Box(s) to length
            }

            override suspend fun encode(value: Box): Payload = StringCodec.encode(value.obj)
        })

        functionServer<Api6>(object : Api6 {
            override suspend fun boo(a: Box): Box = a
        })

        val api = functionClient<Api6>()
        assertEquals(Box("foo"), api.boo(Box("foo")))
    }


    interface Api7 {
        suspend fun boo(a: Int): Int
        suspend fun bar(a: Int): Int
    }

    @Test
    fun multipleFunctions() = runTest {
        functionServer<Api7>(object : Api7 {
            override suspend fun boo(a: Int): Int = a
            override suspend fun bar(a: Int): Int = a
        })

        val api = functionClient<Api7>()
        assertEquals(545, api.boo(545))
        assertEquals(545, api.bar(545))
    }
}
