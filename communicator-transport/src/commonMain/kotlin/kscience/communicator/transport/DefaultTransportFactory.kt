package space.kscience.communicator.transport

import space.kscience.communicator.api.Transport
import space.kscience.communicator.api.TransportFactory
import space.kscience.communicator.zmq.client.ZmqTransport
import kotlin.jvm.Synchronized

/**
 * Standard [TransportFactory] implementation. Currently, it only supports ZeroMQ protocol.
 */
public object DefaultTransportFactory : TransportFactory {
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
