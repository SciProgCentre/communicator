@file:Suppress("KDocMissingDocumentation")

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import space.kscience.communicator.api.ClientEndpoint
import space.kscience.communicator.prettyapi.communicator
import space.kscience.communicator.prettyapi.defaultTransport
import space.kscience.communicator.prettyapi.functionClient
import space.kscience.communicator.prettyapi.functionServer
import space.kscience.communicator.zmq.zmq

@Serializable
data class Structure(val a: Int, val b: Set<Int>)

interface API {
    suspend fun boo(j: Structure): Int
}

fun main(): Unit = communicator(defaultEndpoint = ClientEndpoint("ZMQ", "localhost:8888")) {
    serializableFormat = JSON
    defaultTransport { zmq() }

    functionServer<API>(object : API {
        override suspend fun boo(j: Structure): Int = j.a * j.b.sum()
    })

    val api = functionClient<API>()

    runBlocking {
        println(api.boo(Structure(2, setOf((3)))))
    }
}
