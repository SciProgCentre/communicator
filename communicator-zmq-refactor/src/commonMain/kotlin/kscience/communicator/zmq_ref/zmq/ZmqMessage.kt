package kscience.communicator.zmq_ref.zmq

import kscience.communicator.api_ref.Payload

internal sealed class ZmqMessageContent {
    class StringContent(val value: String): ZmqMessageContent()
    class ByteConent(val value: Payload): ZmqMessageContent()
}

internal expect class ZmqMessage() {
    fun add(msg: String)
    fun add(msg: ByteArray)

    fun pop(): Payload
    //decodeToString() ?
    fun popString(): String
}

internal fun ZmqMessage.add(msg: ZmqMessageContent) {
    when (msg) {
        is ZmqMessageContent.StringContent -> add(msg.value)
        is ZmqMessageContent.ByteConent -> add(msg.value)
    }
}

internal fun ZmqMessage.addAll(vararg msgs: ZmqMessageContent) {
    for (msg in msgs) {
        add(msg)
    }
}