package space.kscience.communicator.demo

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readFully
import kotlinx.coroutines.runBlocking
import space.kscience.communicator.api.*
import space.kscience.communicator.transport.TransportFunctionClient
import space.kscience.communicator.transport.TransportFunctionServer

private val endpoint = Endpoint("ZMQ", "127.0.0.1:8888")

private object Functions : FunctionSet(endpoint) {
    val f by declare(FunctionSpec(LongCoder, LongCoder))
}

/**
 * Launches [TransportFunctionServer] with function f(x) = x^2 + 1 and [TransportFunctionClient] calling that
 * function, calls f from 123, and prints the result.
 */
fun m1ain(): Unit = runBlocking {
    val server = TransportFunctionServer(Functions) {
        it.impl(f) { x -> x * x + 1 }
    }

    val client = TransportFunctionClient()
    println("Calling ${Functions.f}")
    val result = Functions.f(client, 123)
    println("Result is $result")
    server.close()
    client.close()
}
