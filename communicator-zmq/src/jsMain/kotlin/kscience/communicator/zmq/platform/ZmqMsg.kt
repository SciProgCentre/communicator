package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
internal actual class ZmqMsg actual constructor() : Closeable, MutableCollection<ZmqFrame> {
    override val size: Int
        get() = TODO()

    actual fun pop(): ZmqFrame = TODO()
    actual fun send(socket: ZmqSocket): Unit = TODO()
    override fun close(): Unit = TODO()
    actual fun add(data: ByteArray): Boolean = TODO()
    override fun contains(element: ZmqFrame): Boolean = TODO()
    override fun containsAll(elements: Collection<ZmqFrame>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun add(element: ZmqFrame): Boolean = TODO()
    override fun addAll(elements: Collection<ZmqFrame>): Boolean = TODO()
    override fun clear(): Unit = TODO()
    override fun iterator(): MutableIterator<ZmqFrame> = TODO()
    override fun remove(element: ZmqFrame): Boolean = TODO()
    override fun removeAll(elements: Collection<ZmqFrame>): Boolean = TODO()
    override fun retainAll(elements: Collection<ZmqFrame>): Boolean = TODO()

    actual companion object {
        actual fun recvMsg(socket: ZmqSocket): ZmqMsg = TODO()
    }
}
