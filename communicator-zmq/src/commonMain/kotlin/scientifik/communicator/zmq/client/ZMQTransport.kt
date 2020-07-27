package scientifik.communicator.zmq.client

import scientifik.communicator.api.Payload
import scientifik.communicator.api.Transport

class ZMQTransport : Transport {

    override suspend fun respond(address: String, name: String, payload: Payload): Payload {
        TODO("Not yet implemented")
    }
}