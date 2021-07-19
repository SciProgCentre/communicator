package space.kscience.communicator.zmq

import space.kscience.communicator.api.*
import space.kscience.communicator.zmq.client.ZmqTransportClient
import space.kscience.communicator.zmq.server.ZmqTransportServer

/**
 * Creates a new [TransportFactory] which handles ZMQ protocol name by binding it to [ZmqTransportClient] and
 * [ZmqTransportServer].
 */
public fun TransportFactory.zmq(): TransportFactory = object : TransportFactory {
    override fun client(protocol: String): TransportClient? =
        if (protocol == "ZMQ") ZmqTransportClient() else this@zmq.client(protocol)

    override fun server(protocol: String, port: Int): TransportServer? =
        if (protocol == "ZMQ") ZmqTransportServer(port) else this@zmq.server(protocol, port)
}
