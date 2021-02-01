package kscience.communicator.zmq_ref

import kscience.communicator.api_ref.PayloadFunction
import kscience.communicator.api_ref.TransportServer

expect class ZMQWorker{
    fun close()
}

class ZMQTransportServer: TransportServer {
    private val worker = ZMQWorker()

    private val functions: MutableMap<String, PayloadFunction> = hashMapOf()

    override fun register(name: String, function: PayloadFunction) {
        //TODO deside what to do if name is already taken
        functions[name] = function
    }

    override fun unregister(name: String) {
        functions.remove(name)
    }

    override fun close() {
        worker.close()
    }

}