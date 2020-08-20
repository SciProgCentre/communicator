import kotlinx.coroutines.runBlocking
import mu.KLogger
import mu.KotlinLogging
import scientifik.communicator.api.*
import scientifik.communicator.factories.DefaultTransportFactory
import scientifik.communicator.factories.TransportFunctionServer

private val endpoint = Endpoint("ZMQ", ":8888")

private object Functions : FunctionSet(endpoint) {
    val f = declare("f" to FunctionSpec(IntCoder, IntCoder))
}

private val log: KLogger = KotlinLogging.logger { }

fun main() {
    val server = TransportFunctionServer(endpoint).configure(Functions) { it.impl(f) { x -> x * x + 1 } }
    val client = TransportFunctionClient(DefaultTransportFactory)
    log.info { "Calling ${Functions.f}" }

    runBlocking {
        val result = Functions.f(client, 123)
        log.info { "Result is $result" }
    }

    server.close()
    client.close()
}
