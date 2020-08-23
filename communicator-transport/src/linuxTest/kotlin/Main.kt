import kotlinx.coroutines.runBlocking
import kscience.communicator.api.*
import kscience.communicator.transport.DefaultTransportFactory
import kscience.communicator.transport.TransportFunctionClient
import kscience.communicator.transport.TransportFunctionServer
import kotlin.test.Test
import kotlin.test.assertEquals

class Main {
    @Test
    fun main() {
        val endpoint = Endpoint("ZMQ", "127.0.0.1:8080")

        val functions = object : FunctionSet(endpoint) {
            val f = declare("f" to FunctionSpec(IntCoder, IntCoder))
        }

        runBlocking {
            val client = TransportFunctionClient(DefaultTransportFactory)
            val server = TransportFunctionServer(endpoint).configure(functions) { it.impl(f) { x -> x * x + 1 } }
            println("Calling ${functions.f}")
            val result = functions.f(client, 123)
            println("Result is $result")
            assertEquals(123 * 123 + 1, result)
            server.close()
            client.close()
        }
    }
}
