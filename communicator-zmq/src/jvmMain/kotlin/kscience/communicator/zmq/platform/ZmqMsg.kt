package kscience.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.ZMsg

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
internal actual class ZmqMsg(internal val backendMsg: ZMsg) : Closeable, MutableCollection<ZmqFrame> {
    actual constructor() : this(ZMsg())

    override val size: Int
        get() = backendMsg.size

    actual fun add(data: ByteArray): Boolean = backendMsg.add(data)
    override fun add(element: ZmqFrame): Boolean = backendMsg.add(element.backendFrame)
    actual fun pop(): ZmqFrame = ZmqFrame(backendMsg.pop())

    actual fun send(socket: ZmqSocket) {
        backendMsg.send(socket.backendSocket)
    }

    actual override fun close(): Unit = backendMsg.destroy()

    fun add(s: String): Boolean = backendMsg.add(s)

    override operator fun iterator(): MutableIterator<ZmqFrame> {
        val it = backendMsg.iterator()

        return object : MutableIterator<ZmqFrame> {
            override fun hasNext(): Boolean = it.hasNext()
            override fun next(): ZmqFrame = ZmqFrame(it.next())
            override fun remove() = it.remove()
        }
    }

    override operator fun contains(element: ZmqFrame): Boolean = backendMsg.contains(element.backendFrame)

    override fun containsAll(elements: Collection<ZmqFrame>): Boolean =
        backendMsg.containsAll(elements.map(ZmqFrame::backendFrame))

    override fun isEmpty(): Boolean = backendMsg.isEmpty()
    override fun addAll(elements: Collection<ZmqFrame>): Boolean = backendMsg.addAll(elements.map { it.backendFrame })
    override fun clear(): Unit = backendMsg.clear()
    override fun remove(element: ZmqFrame): Boolean = backendMsg.remove(element.backendFrame)

    override fun removeAll(elements: Collection<ZmqFrame>): Boolean =
        backendMsg.removeAll(elements.map(ZmqFrame::backendFrame))

    override fun retainAll(elements: Collection<ZmqFrame>): Boolean =
        backendMsg.retainAll(elements.map(ZmqFrame::backendFrame))

    actual companion object {
        actual fun recvMsg(socket: ZmqSocket): ZmqMsg = ZmqMsg(ZMsg.recvMsg(socket.backendSocket))
    }
}
