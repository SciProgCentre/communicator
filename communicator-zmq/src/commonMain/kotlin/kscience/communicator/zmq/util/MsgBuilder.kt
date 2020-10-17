package kscience.communicator.zmq.util

import kotlinx.io.use
import kscience.communicator.zmq.platform.UniqueID
import kscience.communicator.zmq.platform.ZmqFrame
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal operator fun ZmqMsg.plusAssign(data: ByteArray) {
    add(data)
}

internal class MsgBuilder(private val msg: ZmqMsg) {
    operator fun ByteArray.unaryPlus() {
        msg += this
    }

    operator fun ZmqFrame.unaryPlus() {
        msg += this
    }

    operator fun String.unaryPlus() {
        msg += encodeToByteArray()
    }

    operator fun UniqueID.unaryPlus() {
        msg += bytes
    }
}

internal inline fun sendMsg(socket: ZmqSocket, block: MsgBuilder.() -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

    ZmqMsg().use { msg ->
        MsgBuilder(msg).block()
        msg.send(socket)
    }
}
