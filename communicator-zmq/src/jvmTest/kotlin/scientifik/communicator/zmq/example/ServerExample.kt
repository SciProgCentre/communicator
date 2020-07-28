package scientifik.communicator.zmq.example

import scientifik.communicator.zmq.server.ZMQTransportServer

fun main() {
    val zmqTransportServer = ZMQTransportServer(1234)
    zmqTransportServer.register("foo") { it.map { x -> (x * 2).toByte() }.toByteArray() }
    Thread.sleep(99999)
}