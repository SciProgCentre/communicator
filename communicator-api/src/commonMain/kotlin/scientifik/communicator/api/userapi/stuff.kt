package scientifik.communicator.api.userapi

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import scientifik.communicator.api.Payload
import scientifik.communicator.api.PayloadFunction

data class Order(val result: Deferred<Payload>, val intermediateMessages: Flow<Message>)

class UnknownMessageTypeException(val stringRepr: String): Exception() {
    override val message: String?
        get() = "Message type with \"$stringRepr\" string representation is unknown"
}

class UnexpectedMessageTypeException(val stringRepr: String): Exception() {
    override val message: String?
        get() = "Message type with \"$stringRepr\" string representation is unexpected"
}

class Message(val type: MessageType, val content: Payload) {
    fun toByteArray(): ByteArray {
        return type.toByteArray() + content
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): Message {
            val (type, content) = MessageType.fromByteArray(bytes)
            return Message(type, content)
        }
    }
}

sealed class MessageType(val stringRepr: String) {
    fun toByteArray(): ByteArray {
        val stringBytes = stringRepr.encodeToByteArray()
        return byteArrayOf(stringBytes.size.toByte()) + stringBytes
    }

    class Initial: MessageType("init")
    class Status: MessageType("status")
    class Confirmation: MessageType("confirmation")
    class Abort: MessageType("abort")
    class Success: MessageType("succ")
    class Fail: MessageType("fail")

    companion object {
        fun fromByteArray(bytes: ByteArray): Pair<MessageType, ByteArray> {
            val len = bytes.first()
            val stringBytes = bytes.slice(1 until 1 + len).toByteArray()
            val stringRepr = stringBytes.decodeToString()
            val type = when (stringRepr) {
                Initial().stringRepr -> Initial()
                Status().stringRepr -> Status()
                Confirmation().stringRepr -> Confirmation()
                Abort().stringRepr -> Abort()
                Success().stringRepr -> Success()
                Fail().stringRepr -> Fail()
                else -> throw UnknownMessageTypeException(stringRepr)
            }

            return Pair(type, bytes.slice((1 + len) until bytes.size).toByteArray())
        }
    }
}


// Only one byte for ids for now
class OrderPlacer(val channel: PayloadFunction) {
    private var orderId: Byte = 0

    private fun newId(): ByteArray {
        return byteArrayOf(orderId++)
    }

    fun getId(bytes: ByteArray): Pair<Byte, ByteArray> {

    }

    suspend fun getResponse(msg: Message, id: ByteArray): Pair<Message, Byte> {
        return Message.fromByteArray(channel(id + msg.toByteArray()))
    }

    // For now user HAS to read output flow
    fun placeOrder(content: Payload, intermediateMessages: Flow<Message> = emptyFlow()): Order {
        val thisId = newId()
        val initialMessage = Message(MessageType.Initial(), content)
        var response: Message
        val result = CompletableDeferred<Payload>()
        val inputFlow = flow {
            response = Message.fromByteArray(channel(initialMessage.toByteArray()))
            loop@ while (true) {
                when (response.type) {
                    is MessageType.Success -> {
                        result.complete(response.content)
                    }
                    is MessageType.Fail -> {
                        //TODO(): create exceptions for different reasons of
                        // failure or maybe remove option to fail
                        throw Exception("Got FAIL result")
                    }
                    is MessageType.Initial,
                    is MessageType.Abort,
                    is MessageType.Confirmation -> throw UnexpectedMessageTypeException(response.type.stringRepr)
                    is MessageType.Status -> {
                        emit(response)
                        response = Message.fromByteArray(
                                channel(
                                        Message(
                                                MessageType.Confirmation(), byteArrayOf()).toByteArray()))
                    }
                }
            }
        }
        GlobalScope.launch {
            intermediateMessages.collect {
                val intermediateResponse = Message.fromByteArray(channel(it.toByteArray()))
                println("Got ${intermediateResponse.type.stringRepr} as intermediate response")
            }
        }

        return Order(result, inputFlow)
    }
}

fun <A> receiveOrder(content: Payload, intermediateMessages: Flow<Message>): Pair<A, Flow<Message>> {

}

