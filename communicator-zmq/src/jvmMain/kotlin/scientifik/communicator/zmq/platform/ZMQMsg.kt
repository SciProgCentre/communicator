package scientifik.communicator.zmq.platform

import org.zeromq.ZMsg

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
actual class ZMQMsg(val backupMsg: ZMsg) {

    actual constructor() : this(ZMsg())

    actual fun add(data: ByteArray) {
        backupMsg.add(data)
    }

    actual fun add(frame: ZMQFrame) {
        backupMsg.add(frame.backupFrame)
    }

    actual fun pop(): ZMQFrame = ZMQFrame(backupMsg.pop())

    actual fun send(socket: ZMQSocket) {
        backupMsg.send(socket.backupSocket)
    }

    actual fun close() {
        backupMsg.destroy()
    }

}