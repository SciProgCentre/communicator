package scientifik.communicator.zmq.example

import kotlinx.coroutines.runBlocking
import scientifik.communicator.zmq.client.ZMQTransport

fun main() {
    val zmqTransport = ZMQTransport()
    val result = runBlocking {
        zmqTransport.respond("localhost:1234", "foo", byteArrayOf(1, 2, 3, 4))
    }
    println(result.contentToString())
}