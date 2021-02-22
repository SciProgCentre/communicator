package kscience.communicator.zmq_ref

import kscience.communicator.zmq_ref.zmq.ZmqContext
import kotlin.concurrent.thread

fun main() {
    thread {
        println("server starting...")
        runServer(ZmqContext(), "test-server")
    }

    thread {
        println("client starting...")
        runClient("test-server")
    }
    readLine()
}