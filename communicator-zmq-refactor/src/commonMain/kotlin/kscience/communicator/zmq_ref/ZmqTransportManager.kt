package kscience.communicator.zmq_ref

import kotlinx.io.Closeable
import kscience.communicator.api_ref.Payload
import kscience.communicator.zmq_ref.zmq.*
import kscience.communicator.zmq_ref.zmq.ZmqContext
import kscience.communicator.zmq_ref.zmq.ZmqMessage
import kscience.communicator.zmq_ref.zmq.ZmqProtocol
import kscience.communicator.zmq_ref.zmq.ZmqSocket
import kscience.communicator.zmq_ref.zmq.makeZmqAddress


/**
 * Should generate different ids each time and be thread-safe
 */
expect class idGenerator() {
    fun getNext(): String
}

/**
 * Object because only one ZMQ context is needed
 */
object ZmqTransportManager: Closeable {
    private val context = ZmqContext()
    private val loop = context.createLoop()

    // should be constexpr, but kotlin doesn't has it yet :(
    private val shutdownEndpoint = makeZmqAddress(
            ZmqProtocol.inproc,
            "ZMQTransportManager/death"
    )
    private val registerTransportEndpoint = makeZmqAddress(
            ZmqProtocol.inproc,
            "ZMQTransportManager/transport_reg"
    )
    private val fServerRegisterEndpoint = makeZmqAddress(
            ZmqProtocol.inproc,
            "ZMQTransportManager/fserver_reg"
    )
    private val baseTransportMessagesEndpoint = makeZmqAddress(
            ZmqProtocol.inproc,
            "ZMQTransportManager/transport/"
    )

    private val transportIdGenerator = idGenerator()
    private fun generateTransportId(): String {
        return transportIdGenerator.getNext()
    }

    // TODO: should this really be thread-safe?
    private val requestIdGenerator = idGenerator()
    private fun generateRequestId(): String {
        return requestIdGenerator.getNext()
    }

    // TODO: decide witch type of map to use exactly
    private val activeConnections = mutableMapOf<String, ZmqSocket>()

    private fun remoteCall(
            destination: String,
            name: String,
            argument: Payload,
            callback: (ZmqMessage) -> Unit) {
        val remoteServer = activeConnections.getOrPut(destination) {
            val fServer = context.createSocket(ZmqSocketType.DEALER)
            fServer.connect(destination)
            loop.add(fServer) {
                val answer = fServer.recv()
                callback(answer)
            }
            fServer
        }
        val request = ZmqMessage()
        request.add("QUERY")
        request.add(generateRequestId())
        request.add(argument)
        request.add(name)

        remoteServer.send(request, false)
    }

    init {
        val transportSockets = mutableListOf<ZmqSocket>()

        val transportRegistrator = context.createSocket(ZmqSocketType.PULL)
        transportRegistrator.bind(registerTransportEndpoint)
        loop.add(transportRegistrator) {
            // this is blocking, but should return immediately
            val request = transportRegistrator.recv()
            val callbackAddress = request.popString()

            val concreteTransportListener = context.createSocket(ZmqSocketType.PAIR)
            concreteTransportListener.connect(callbackAddress)
            loop.add(concreteTransportListener) {
                val forwardRequest = concreteTransportListener.recv()
                val destination = forwardRequest.popString()
                val fName = forwardRequest.popString()
                val payload = forwardRequest.pop()
                remoteCall(destination, fName, payload) {
                    concreteTransportListener.send(it)
                }
            }

            transportSockets.add(concreteTransportListener)

        }

        loop.start()
    }

    private fun getTransportCallbackAddress(transportId: String): String {
        return baseTransportMessagesEndpoint + transportId
    }

    //thread-safe
    internal fun create(): ZmqTransport {
        val transportId = generateTransportId()
        return ZmqTransport(
            context,
            registerTransportEndpoint,
            getTransportCallbackAddress(transportId)
        )
    }

    override fun close() {
        loop.close()
        for (strToSocket in activeConnections) {
            strToSocket.value.close()
        }
    }
}