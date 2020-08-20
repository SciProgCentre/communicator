package scientifik.communicator.factories

import scientifik.communicator.api.Transport
import scientifik.communicator.api.TransportFactory
import scientifik.communicator.zmq.client.ZmqTransport
import kotlin.jvm.Synchronized

object DefaultTransportFactory : TransportFactory {
    private val transports: MutableMap<String, Transport> = hashMapOf()

    @Synchronized
    override fun get(protocol: String): Transport? {
        val currentTransport = transports[protocol]

        if (currentTransport == null) {
            val newTransport = when (protocol) {
                "ZMQ" -> ZmqTransport()
                else -> null
            }

            if (newTransport != null) transports[protocol] = newTransport
            return newTransport
        }

        return currentTransport
    }
}
