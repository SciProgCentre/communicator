package kscience.communicator.zmq_ref

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kscience.communicator.zmq_ref.zmq.*
import kscience.communicator.zmq_ref.zmq.ZmqContext
import kscience.communicator.zmq_ref.zmq.ZmqProtocol
import kscience.communicator.zmq_ref.zmq.ZmqReactor
import kscience.communicator.zmq_ref.zmq.ZmqSocket
import kscience.communicator.zmq_ref.zmq.makeZmqAddress

/**
 * Object because only one ZMQ context is needed
 */
object ZmqTransportManager {
    private val context = ZmqContext()

    // should be constexpr, but kotlin doesn't have it yet :(
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

    private fun addTransportListener(
            reactor: ZmqReactor,
            transportSockets: MutableList<ZmqSocket>,
            registrator: ZmqSocket
    ) {

    }

    init {
        GlobalScope.launch {
            var alive = true
            val reactor = ZmqReactor()
            val transportSockets = mutableListOf<ZmqSocket>()
            val transportRegistrator = context.createSocket(ZmqSocketType.PULL)
            transportRegistrator.bind(registerTransportEndpoint)

            val shutdown = context.createSocket(ZmqSocketType.REPLY)
            shutdown.bind(shutdownEndpoint)
            reactor.add(shutdown) {
                shutdown.recv()
                alive = false
                shutdown.send("")
            }

            // change to loop?
            while (alive) {
                reactor.poll()
            }
        }
    }
}