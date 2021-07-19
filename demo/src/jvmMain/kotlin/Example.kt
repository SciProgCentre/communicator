import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import space.kscience.communicator.api.*
import space.kscience.communicator.zmq.zmq

private val endpoint = ClientEndpoint("ZMQ", "127.0.0.1:8888")

@Serializable
data class MyArguments(val arg1: Int, val arg2: Double)

private object Functions : FunctionSet(endpoint) {
    val f by declare(JsonCodec<MyArguments>(), IntCodec)
}

/**
 * Launches [FunctionServer] with function f(x) = x^2 + 1 and [FunctionClient] calling that
 * function, calls f from 123, and prints the result.
 */
fun main(): Unit = runBlocking {
    val server = FunctionServer(Functions, TransportFactory.zmq()) {
        it.impl(f) { (arg1, arg2) -> arg1 * arg2.toInt() }
    }

    val client = FunctionClient(TransportFactory.zmq())
    println("Calling ${Functions.f}")
    val result: Any = Functions.f(client, MyArguments(1, 2.0))
    println("Result is $result")
    client.close()
    server.close()
}
