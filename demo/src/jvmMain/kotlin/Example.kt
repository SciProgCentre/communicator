import kotlinx.coroutines.runBlocking
import space.kscience.communicator.api.*
import space.kscience.communicator.zmq.withZmq

private val endpoint = ClientEndpoint("ZMQ", "127.0.0.1:8888")

private object Functions : FunctionSet(endpoint) {
    val f by declare(IntCoder, IntCoder)
    val g by declare(IntCoder, StringCoder)
}

/**
 * Launches [TransportFunctionServer] with function f(x) = x^2 + 1 and [TransportFunctionClient] calling that
 * function, calls f from 123, and prints the result.
 */
fun main(): Unit = runBlocking {
    val server = TransportFunctionServer(Functions, TransportServerFactory.withZmq()) {
        it.impl(f) { x -> x * x + 1 }
        it.impl(g) { x -> "a".repeat(x) }
    }

    val client = TransportFunctionClient(TransportClientFactory.withZmq())
    println("Calling ${Functions.f}")
    var result: Any = Functions.f(client, 123)
    println("Result is $result")
    println("Calling ${Functions.g}")
    result = Functions.g(client, 55)
    println("Result is $result")
    client.close()
    server.close()
}
