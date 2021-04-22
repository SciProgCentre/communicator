package space.kscience.communicator.rsocket

import kotlinx.coroutines.runBlocking
import space.kscience.communicator.api.*
import space.kscience.communicator.transport.TransportFunctionClient
import space.kscience.communicator.transport.TransportFunctionServer
import space.kscience.communicator.zmq.client.ZmqTransport

private val endpoint = Endpoint("ZMQ", "127.0.0.1:8888")

private object Functions : FunctionSet(endpoint) {
    val f by declare(FunctionSpec(IntCoder, IntCoder))
    val g by declare(FunctionSpec(IntCoder, StringCoder))
}

/**
 * Launches [TransportFunctionServer] with function f(x) = x^2 + 1 and [TransportFunctionClient] calling that
 * function, calls f from 123, and prints the result.
 */
public fun main(): Unit = runBlocking {
    val server = TransportFunctionServer(Functions) {
        it.impl(f) { x -> x * x + 1 }
        it.impl(g) { x -> "a".repeat(x) }
    }
    val jsProxy = JsProxyServer("127.0.0.1", 6789, ZmqTransport(), mapOf(Pair("f", "127.0.0.1:8888")))
    println("starting proxy...")
    jsProxy.start()
    server.close()
}
