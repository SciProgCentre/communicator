package kscience.communicator.zmq_ref.zmq

import kscience.communicator.api_ref.Payload
import org.zeromq.ZMsg

// [actualMessage] is internal, because in jeromq message sends itself for some reason
internal actual class ZmqMessage(internal val actualMessage: ZMsg) {
    actual constructor() : this(ZMsg())

    actual fun add(msg: String) {
        actualMessage.add(msg)
    }

    actual fun add(msg: ByteArray) {
        actualMessage.add(msg)
    }

    actual fun pop(): Payload {
        return actualMessage.pop().data
    }

    actual fun popString(): String {
        return actualMessage.popString()
    }
}
