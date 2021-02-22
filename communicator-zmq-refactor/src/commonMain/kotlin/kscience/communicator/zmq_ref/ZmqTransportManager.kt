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
 * Object because only one ZMQ context is needed
 */
//TODO: separate thread-safe and not thread-safe parts
//TODO: remove used callbacks
object ZmqTransportManager: Closeable {
    private val context = ZmqContext()

    private val worker = ZmqLoopJob(context, makeZmqAddress(ZmqProtocol.inproc, "ZMQTransportManager/worker"))

    // should be constexpr, but kotlin doesn't has it yet :(
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

    private val transportIdGenerator = IdGenerator()
    private fun generateTransportId(): String {
        return transportIdGenerator.getNext()
    }

    // TODO: should this really be thread-safe?
    private val requestIdGenerator = IdGenerator()
    private fun generateRequestId(): String {
        return requestIdGenerator.getNext()
    }

    private fun remoteCall(
            activeConnections: MutableMap<String, ZmqSocket>,
            loop: ZmqLoop,
            destination: String,
            name: String,
            argument: Payload,
            callbacks: MutableMap<String, (ZmqMessage) -> Unit>,
            callback: (ZmqMessage) -> Unit) {
        val remoteServer = activeConnections.getOrPut(destination) {
            // dealer, because several requests to one fServer are possible
            val fServer = context.createSocket(ZmqSocketType.DEALER)
            fServer.connect(destination)
            loop.add(fServer) {
                val answer = fServer.recv()
                val id = answer.popString()
                //TODO: decide what to do if id is unknown
                (callbacks[id])?.invoke(answer)
            }
            fServer
        }

        val request = ZmqMessage()
        val requestId = generateRequestId()
        callbacks[requestId] = callback
        request.add(requestId)
        request.add("QUERY")
        request.add(argument)
        request.add(name)

        remoteServer.send(request, false)
    }

    init {
        worker.start { loop ->
            // this exists only in one thread

            // TODO: decide witch type of map to use exactly
            val activeConnections = mutableMapOf<String, ZmqSocket>()
            val transportSockets = mutableListOf<ZmqSocket>()
            val callbacks = mutableMapOf<String, (ZmqMessage) -> Unit>()

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
                    remoteCall(activeConnections, loop, destination, fName, payload, callbacks) {
                        concreteTransportListener.send(it)
                    }
                }
                transportSockets.add(concreteTransportListener)
            }
            loop.start()

            // After loop is closed
            for (strToSocket in activeConnections) {
                strToSocket.value.close()
            }
        }
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
        worker.close()
    }
}
