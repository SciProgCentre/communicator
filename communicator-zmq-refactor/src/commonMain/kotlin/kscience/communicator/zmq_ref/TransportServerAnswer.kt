package kscience.communicator.zmq_ref

import kscience.communicator.api_ref.Payload
import kscience.communicator.zmq_ref.zmq.ZmqMessage

internal object TransportServerAnswer {
    private fun withId(id: String, type: String): ZmqMessage {
        return ZmqMessage().apply {
            add(type)
            add(id)
        }
    }

    fun success(id: String, result: Payload): ZmqMessage {
        return withId(id, "RESPONSE_RESULT").apply { add(result) }
    }

    fun unknownFunction(id: String, name: String): ZmqMessage {
        return withId(id, "RESPONSE_UNKNOWN_FUNCTION").apply { add(name) }
    }

    fun functionException(id: String, what: String): ZmqMessage {
        return withId(id, "RESPONSE_EXCEPTION").apply { add(what) }
    }

    fun unknownCommand(command: String): ZmqMessage {
        return ZmqMessage().apply { add("INCORRECT_COMMAND"); add(command) }
    }

}
