package space.kscience.communicator.zmq

import space.kscience.communicator.api.TransportClientFactory
import space.kscience.communicator.api.TransportServerFactory
import space.kscience.communicator.zmq.client.ZmqTransportClient
import space.kscience.communicator.zmq.server.ZmqTransportServer

public fun TransportClientFactory.withZmq(): TransportClientFactory = TransportClientFactory {
    if (it == "ZMQ") ZmqTransportClient() else this[it]
}

public fun TransportServerFactory.withZmq(): TransportServerFactory = TransportServerFactory { protocol, port ->
    if (protocol == "ZMQ") ZmqTransportServer(port) else this[protocol, port]
}
