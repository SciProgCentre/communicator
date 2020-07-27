package scientifik.communicator.zmq.server

import scientifik.communicator.api.PayloadFunction
import scientifik.communicator.api.TransportServer

class ZMQTransportServer(override val port: Int) : TransportServer {

    override fun register(name: String, function: PayloadFunction) {
        TODO("Not yet implemented")
    }

    override fun unregister(name: String) {
        TODO("Not yet implemented")
    }

}