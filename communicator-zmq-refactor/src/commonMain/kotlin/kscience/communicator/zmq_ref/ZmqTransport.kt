package kscience.communicator.zmq_ref

import kscience.communicator.api_ref.Payload
import kscience.communicator.api_ref.Transport
import kscience.communicator.zmq_ref.zmq.*
import kscience.communicator.zmq_ref.zmq.ZmqSocketType
import kscience.communicator.zmq_ref.zmq.ZmqContext
import kscience.communicator.zmq_ref.zmq.ZmqMessage
import kscience.communicator.zmq_ref.zmq.ZmqSocket

class RemoteFunctionException(what: String): Exception(what)
class UnknownRemoteFunction(what: String): Exception(what)
class IncorrectReplyType(type: String): Exception("Reply type $type is not supported")


internal class ZmqTransport(
        context: ZmqContext,
        registerEndpoint: String,
        callbackEndpoint: String
): Transport {

    private val managerSocket: ZmqSocket = context.createSocket(ZmqSocketType.PAIR)
    init {
        managerSocket.bind(callbackEndpoint)
        val registerSocket = context.createSocket(ZmqSocketType.PUSH)
        registerSocket.connect(registerEndpoint)

        val registerRequest = ZmqMessage()
        registerRequest.add(callbackEndpoint)
        registerSocket.send(registerRequest)
    }

    override suspend fun respond(address: String, name: String, payload: Payload): Payload {
        val request = ZmqMessage()
        request.add(address)
        request.add(name)
        request.add(payload)

        managerSocket.suspendSend(request)
        val reply = managerSocket.suspendRecv()
        val type = reply.popString()
        val id = reply.popString()

        return when(type) {
            "RESPONSE_RESULT" -> {reply.pop()}
            "RESPONSE_EXCEPTION" -> {
                val what = reply.popString()
                throw RemoteFunctionException(what)
            }
            "RESPONSE_UNKNOWN_FUNCTION" -> {
                val functionName = reply.popString()
                throw UnknownRemoteFunction("Function \"$functionName\" is not supported by functional server")
            }
            "CODER_IDENTITY_FOUND" -> {
                //TODO
                throw IncorrectReplyType(type)
            }
            "CODER_IDENTITY_NOT_FOUND" -> {
                //TODO
                throw IncorrectReplyType(type)
            }
            "QUERY_RECEIVED" -> {
                //TODO
                throw IncorrectReplyType(type)
            }
            else -> {
                throw IncorrectReplyType(type)
            }
        }
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}