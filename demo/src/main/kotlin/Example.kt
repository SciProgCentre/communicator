import kotlinx.coroutines.runBlocking
import kscience.communicator.api.*
import kscience.communicator.transport.DefaultTransportFactory
import kscience.communicator.transport.TransportFunctionClient
import kscience.communicator.transport.TransportFunctionServer

private val endpoint = Endpoint("ZMQ", ":8888")

private object Functions : FunctionSet(endpoint) {
    val f = declare("f" to FunctionSpec(IntCoder, IntCoder))
}

/**
 * Launches [TransportFunctionServer] with function f(x) = x^2 + 1 and [TransportFunctionClient] calling that
 * function, calls f from 123, and prints the result.
 */
fun main(): Unit = runBlocking {
    val server = TransportFunctionServer(endpoint).configure(Functions) {
        it.impl(f) { x -> x * x + 1 }
    }

    val client = TransportFunctionClient(DefaultTransportFactory)
    println("Calling ${Functions.f}")
    val result = Functions.f(client, 123)
    println("Result is $result")
    server.close()
    client.close()
}
