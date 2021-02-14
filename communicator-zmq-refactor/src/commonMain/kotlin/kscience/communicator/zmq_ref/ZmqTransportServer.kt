package kscience.communicator.zmq_ref


import kotlinx.coroutines.Dispatchers
import kscience.communicator.api_ref.Payload
import kscience.communicator.api_ref.PayloadFunction
import kscience.communicator.api_ref.TransportServer
import kscience.communicator.zmq_ref.zmq.ZmqContext
import kscience.communicator.zmq_ref.zmq.ZmqMessage
import kscience.communicator.zmq_ref.zmq.ZmqSocketType

// TODO: rethink logic, so everything not thread-safe is _definetely_ executed on
internal class ZMQTransportServer(context: ZmqContext, endpoint: String): TransportServer {
    private val workerLoop = context.createLoop()
    init {

        val listenerSocket = context.createSocket(ZmqSocketType.ROUTER)
        listenerSocket.bind(endpoint)

        workerLoop.addSuspend(listenerSocket) {
            val request = listenerSocket.recv()
            val deferredAnswer = runBlocking { processRequest(request) }
            listenerSocket.send()
        }
    }

    private val functions: MutableMap<String, PayloadFunction> = hashMapOf()

    override fun register(name: String, function: PayloadFunction) {
        //TODO deside what to do if name is already taken
        functions[name] = function
    }

    override fun unregister(name: String) {
        functions.remove(name)
    }

    override fun close() {}



    private suspend fun processRequest(request: ZmqMessage): ZmqMessage {
        val identity = request.pop()
        val command = request.popString()
        return when (command) {
            "QUERY" -> {
                val requestId = request.popString()
                val arg = request.pop()
                val fName = request.popString()
                computeQuery(requestId, fName, arg)
            }
            else -> {
                TransportServerAnswer.unknownCommand()
            }
        }
    }

    private suspend fun computeQuery(requestId: String, fName: String, argument: Payload): ZmqMessage {
        val function = functions[fName]
        return if (function == null) {
            return TransportServerAnswer.unknownFunction(requestId, fName)
        } else {
            try {
                val result = function(argument)
                TransportServerAnswer.success(requestId, result)
            } catch (e: Exception) {
                TransportServerAnswer.functionException(requestId, e.toString())
            }

        }

    }

}