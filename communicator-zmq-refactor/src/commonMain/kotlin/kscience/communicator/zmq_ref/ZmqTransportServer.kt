package kscience.communicator.zmq_ref


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kscience.communicator.api_ref.Payload
import kscience.communicator.api_ref.PayloadFunction
import kscience.communicator.api_ref.TransportServer
import kscience.communicator.zmq_ref.zmq.ZmqContext
import kscience.communicator.zmq_ref.zmq.ZmqLoopJob
import kscience.communicator.zmq_ref.zmq.ZmqMessage
import kscience.communicator.zmq_ref.zmq.ZmqSocketType


internal class ZMQTransportServer(context: ZmqContext, listenEndpoint: String, workerEndpoint: String): TransportServer {
    private val worker = ZmqLoopJob(context, workerEndpoint)
    private val functionExecutionDispatcher = Dispatchers.Default

    init {
        worker.start { loop ->
            val listenerSocket = context.createSocket(ZmqSocketType.ROUTER)
            listenerSocket.bind(listenEndpoint)

            loop.add(listenerSocket) {
                val request = listenerSocket.recv()
                GlobalScope.launch(functionExecutionDispatcher) {
                    val answer = processRequest(request)
                    listenerSocket.send(answer, false)
                }
            }
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

    override fun close() {
        worker.close()
    }

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
                TransportServerAnswer.unknownCommand(command)
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