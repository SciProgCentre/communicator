package kscience.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.ZMsg
import java.util.*

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
internal actual class ZmqMsg(internal val backendMsg: ZMsg) : Closeable, MutableCollection<ZmqFrame>, Deque<ZmqFrame> {
    actual constructor() : this(ZMsg())

    override val size: Int
        get() = backendMsg.size

    actual fun add(data: ByteArray): Boolean = backendMsg.add(data)
    override fun add(element: ZmqFrame): Boolean = backendMsg.add(element.backendFrame)
    actual override fun pop(): ZmqFrame = ZmqFrame(backendMsg.pop())

    actual fun send(socket: ZmqSocket) {
        backendMsg.send(socket.backendSocket)
    }

    override fun close(): Unit = backendMsg.destroy()

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

    override fun remove(): ZmqFrame = ZmqFrame(backendMsg.remove())
    override fun peekLast(): ZmqFrame = ZmqFrame(backendMsg.peekLast())
    override fun element(): ZmqFrame = ZmqFrame(backendMsg.element())
    override fun push(e: ZmqFrame): Unit = backendMsg.push(e.backendFrame)
    override fun getLast(): ZmqFrame = ZmqFrame(backendMsg.last)
    override fun addLast(e: ZmqFrame): Unit = backendMsg.addLast(e.backendFrame)
    override fun addFirst(e: ZmqFrame): Unit = backendMsg.addFirst(e.backendFrame)
    override fun offer(e: ZmqFrame): Boolean = backendMsg.offer(e.backendFrame)
    override fun peek(): ZmqFrame = ZmqFrame(backendMsg.peek())
    override fun offerLast(e: ZmqFrame): Boolean = backendMsg.offerLast(e.backendFrame)
    override fun removeFirst(): ZmqFrame = ZmqFrame(backendMsg.removeFirst())
    override fun getFirst(): ZmqFrame = ZmqFrame(backendMsg.first)
    override fun removeLastOccurrence(o: Any?): Boolean = backendMsg.removeLastOccurrence(o)
    override fun peekFirst(): ZmqFrame = ZmqFrame(backendMsg.peekFirst())
    override fun removeLast(): ZmqFrame = ZmqFrame(backendMsg.removeLast())
    override fun offerFirst(e: ZmqFrame): Boolean = backendMsg.offerFirst(e.backendFrame)
    override fun pollFirst(): ZmqFrame = ZmqFrame(backendMsg.pollFirst())
    override fun pollLast(): ZmqFrame = ZmqFrame(backendMsg.pollLast())
    override fun removeFirstOccurrence(o: Any?): Boolean = backendMsg.removeFirstOccurrence(o)
    override fun poll(): ZmqFrame = ZmqFrame(backendMsg.poll())

    override fun descendingIterator(): MutableIterator<ZmqFrame> {
        val it = backendMsg.descendingIterator()

        return object : MutableIterator<ZmqFrame> {
            override fun hasNext(): Boolean = it.hasNext()
            override fun next(): ZmqFrame = ZmqFrame(it.next())
            override fun remove() = it.remove()
        }
    }

    actual companion object {
        actual fun recvMsg(socket: ZmqSocket): ZmqMsg = ZmqMsg(ZMsg.recvMsg(socket.backendSocket))
    }
}
