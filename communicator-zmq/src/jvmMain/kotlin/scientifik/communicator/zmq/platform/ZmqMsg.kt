package scientifik.communicator.zmq.platform

import org.zeromq.ZMsg

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
actual class ZmqMsg(val backupMsg: ZMsg) {

    actual constructor() : this(ZMsg())

    actual fun add(data: ByteArray) {
        backupMsg.add(data)
    }

    actual fun add(frame: ZmqFrame) {
        backupMsg.add(frame.backupFrame)
    }

    actual fun pop(): ZmqFrame = ZmqFrame(backupMsg.pop())

    actual fun send(socket: ZmqSocket) {
        backupMsg.send(socket.backupSocket)
    }

    actual fun close() {
        backupMsg.destroy()
    }

}