package scientifik.communicator.zmq.platform

import kotlinx.io.Closeable

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
internal actual class ZmqMsg actual constructor() : Closeable {
    actual fun add(data: ByteArray): Unit = TODO()
    actual fun add(frame: ZmqFrame): Unit = TODO()
    actual fun pop(): ZmqFrame = TODO()
    actual fun send(socket: ZmqSocket): Unit = TODO()
    actual override fun close(): Unit = TODO()
}
