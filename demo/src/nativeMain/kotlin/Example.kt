import kotlinx.coroutines.runBlocking
import space.kscience.communicator.api.*
import space.kscience.communicator.transport.TransportFunctionClient
import space.kscience.communicator.transport.TransportFunctionServer

private val endpoint = Endpoint("ZMQ", "127.0.0.1:8888")

private object Functions : FunctionSet(endpoint) {
    val f by declare(FunctionSpec(IntCoder, IntCoder))
    val g by declare(FunctionSpec(IntCoder, StringCoder))
}

/**
 * Launches [TransportFunctionServer] with function f(x) = x^2 + 1 and [TransportFunctionClient] calling that
 * function, calls f from 123, and prints the result.
 */
fun main(): Unit = runBlocking {
    val server = TransportFunctionServer(Functions) {
        it.impl(f) { x -> x * x + 1 }
        it.impl(g) { x -> "a".repeat(x) }
    }

    val client = TransportFunctionClient()
    println("Calling ${Functions.f}")
    var result: Any = Functions.f(client, 123)
    println("Result is $result")
    println("Calling ${Functions.g}")
    result = Functions.g(client, 55)
    println("Result is $result")
    server.close()
    client.close()
}
