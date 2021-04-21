package space.kscience.communicator.rsocket

import space.kscience.communicator.zmq.client.ZmqTransport

public suspend fun main() {
    val jsProxy = JsProxyServer("127.0.0.1", 6789, ZmqTransport(), mapOf(Pair("", "127.0.0.1:8888")))
    jsProxy.start()
}
