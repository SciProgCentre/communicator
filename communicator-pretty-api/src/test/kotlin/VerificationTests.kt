package space.kscience.communicator.prettyapi

import kotlin.test.Test
import kotlin.test.assertFails

internal class VerificationTests {
    class Api1

    @Test
    fun nonInterfaceClass() = runTest {
        assertFails { functionServer(Api1()) }
    }

    interface Api2<T>

    @Test
    fun genericClass() = runTest {
        assertFails { functionServer<Api2<*>>(object : Api2<Any> {}) }
    }


    interface Api3 {
        suspend fun default(): Unit = throw NotImplementedError()
    }

    @Test
    fun concreteFunction() = runTest {
        assertFails { functionServer<Api3>(object : Api3 {}) }
    }


    interface Api4 {
        suspend fun x()
        suspend fun x(i: Int)
    }

    @Test
    fun repeatingName() = runTest {
        assertFails {
            functionServer<Api4>(object : Api4 {
                override suspend fun x() = throw NotImplementedError()
                override suspend fun x(i: Int) = throw NotImplementedError()
            })
        }
    }

    interface Api5 {
        suspend fun <T> x()
    }

    @Test
    fun genericFunction() = runTest {
        assertFails {
            functionServer<Api5>(object : Api5 {
                override suspend fun <T> x() = throw NotImplementedError()
            })
        }
    }

    interface Api6 {
        suspend fun Any.x()
    }

    @Test
    fun extensionFunction() = runTest {
        assertFails {
            functionServer<Api6>(object : Api6 {
                override suspend fun Any.x() = throw NotImplementedError()
            })
        }
    }

    interface Api7 {
        val x: Int
    }

    @Test
    fun property() = runTest {
        assertFails {
            functionServer<Api7>(object : Api7 {
                override val x: Int
                    get() = throw NotImplementedError()
            })
        }
    }

    interface Api8 {
        val Int.x: Int
    }

    @Test
    fun extensionProperty() = runTest {
        assertFails {
            functionServer<Api8>(object : Api8 {
                override val Int.x: Int
                    get() = throw NotImplementedError()
            })
        }
    }

    interface Api9 {
        fun x()
    }

    @Test
    fun notSuspendingFunction() = runTest {
        assertFails {
            functionServer<Api9>(object : Api9 {
                override fun x() = throw NotImplementedError()
            })
        }
    }

    interface Api10 {
        suspend fun x(optional: Int = 1)
    }

    @Test
    fun optionalParameter() = runTest {
        assertFails {
            functionServer<Api10>(object : Api10 {
                override suspend fun x(optional: Int): Unit = throw NotImplementedError()
            })
        }
    }


    interface Api11 {
        suspend fun x(x: String? )
    }

    @Test
    fun nullableType() = runTest {
        assertFails {
            functionServer<Api11>(object : Api11 {
                override suspend fun x(x: String?) = throw NotImplementedError()
            })
        }
    }
}
