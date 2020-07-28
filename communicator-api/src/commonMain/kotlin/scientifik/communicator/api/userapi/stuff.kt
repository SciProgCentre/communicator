package scientifik.communicator.api.userapi

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

suspend fun placeOrder(content: Payload, channel: PayloadFunction): Order {
    val initialMessage = Message(MessageType.Initial(), content)
    var response = Message.fromByteArray(channel(initialMessage.toByteArray()))
    val result = CompletableDeferred<Payload>()
    val flow = flow {
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
    return Order(result, flow)
}
