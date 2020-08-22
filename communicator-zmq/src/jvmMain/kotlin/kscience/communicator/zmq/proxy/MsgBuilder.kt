package kscience.communicator.zmq.proxy

import kscience.communicator.zmq.platform.ZmqFrame
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal class MsgBuilder(private val msg: ZmqMsg) {
    operator fun ByteArray.unaryPlus() {
        msg.add(this)
    }

    operator fun ZmqFrame.unaryPlus() {
        msg.add(this)
    }

    operator fun String.unaryPlus() {
        msg.add(this)
    }
}

internal inline fun sendMsg(socket: ZmqSocket, block: MsgBuilder.() -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val msg = ZmqMsg()
    MsgBuilder(msg).block()
    msg.send(socket)
}
