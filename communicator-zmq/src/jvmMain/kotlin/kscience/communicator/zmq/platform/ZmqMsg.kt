package kscience.communicator.zmq.platform

import kotlinx.io.Closeable
import org.zeromq.ZMsg
import java.util.*

/** zmsg_t object (CZMQ). Constructor must create it via its init method. */
internal actual class ZmqMsg(internal val handle: ZMsg) : Closeable, MutableCollection<ZmqFrame>, Deque<ZmqFrame> {
    actual constructor() : this(ZMsg())

    override val size: Int
        get() = handle.size

    actual fun add(data: ByteArray): Boolean = handle.add(data)
    override fun add(element: ZmqFrame): Boolean = handle.add(element.handle)
    actual override fun pop(): ZmqFrame = ZmqFrame(handle.pop())

    actual fun send(socket: ZmqSocket) {
        handle.send(socket.handle)
    }

    actual override fun close(): Unit = handle.destroy()

    fun add(s: String): Boolean = handle.add(s)

    override operator fun iterator(): MutableIterator<ZmqFrame> {
        val it = handle.iterator()

        return object : MutableIterator<ZmqFrame> {
            override fun hasNext(): Boolean = it.hasNext()
            override fun next(): ZmqFrame = ZmqFrame(it.next())
            override fun remove() = it.remove()
        }
    }

    override operator fun contains(element: ZmqFrame): Boolean = handle.contains(element.handle)

    override fun containsAll(elements: Collection<ZmqFrame>): Boolean =
        handle.containsAll(elements.map(ZmqFrame::handle))

    override fun isEmpty(): Boolean = handle.isEmpty()
    override fun addAll(elements: Collection<ZmqFrame>): Boolean = handle.addAll(elements.map { it.handle })
    override fun clear(): Unit = handle.clear()
    override fun remove(element: ZmqFrame): Boolean = handle.remove(element.handle)

    override fun removeAll(elements: Collection<ZmqFrame>): Boolean =
        handle.removeAll(elements.map(ZmqFrame::handle))

    override fun retainAll(elements: Collection<ZmqFrame>): Boolean =
        handle.retainAll(elements.map(ZmqFrame::handle))

    override fun remove(): ZmqFrame = ZmqFrame(handle.remove())
    override fun peekLast(): ZmqFrame = ZmqFrame(handle.peekLast())
    override fun element(): ZmqFrame = ZmqFrame(handle.element())
    override fun push(e: ZmqFrame): Unit = handle.push(e.handle)
    override fun getLast(): ZmqFrame = ZmqFrame(handle.last)
    override fun addLast(e: ZmqFrame): Unit = handle.addLast(e.handle)
    override fun addFirst(e: ZmqFrame): Unit = handle.addFirst(e.handle)
    override fun offer(e: ZmqFrame): Boolean = handle.offer(e.handle)
    override fun peek(): ZmqFrame = ZmqFrame(handle.peek())
    override fun offerLast(e: ZmqFrame): Boolean = handle.offerLast(e.handle)
    override fun removeFirst(): ZmqFrame = ZmqFrame(handle.removeFirst())
    override fun getFirst(): ZmqFrame = ZmqFrame(handle.first)
    override fun removeLastOccurrence(o: Any?): Boolean = handle.removeLastOccurrence(o)
    override fun peekFirst(): ZmqFrame = ZmqFrame(handle.peekFirst())
    override fun removeLast(): ZmqFrame = ZmqFrame(handle.removeLast())
    override fun offerFirst(e: ZmqFrame): Boolean = handle.offerFirst(e.handle)
    override fun pollFirst(): ZmqFrame = ZmqFrame(handle.pollFirst())
    override fun pollLast(): ZmqFrame = ZmqFrame(handle.pollLast())
    override fun removeFirstOccurrence(o: Any?): Boolean = handle.removeFirstOccurrence(o)
    override fun poll(): ZmqFrame = ZmqFrame(handle.poll())

    override fun descendingIterator(): MutableIterator<ZmqFrame> {
        val it = handle.descendingIterator()

        return object : MutableIterator<ZmqFrame> {
            override fun hasNext(): Boolean = it.hasNext()
            override fun next(): ZmqFrame = ZmqFrame(it.next())
            override fun remove() = it.remove()
        }
    }

    actual companion object {
        actual fun recvMsg(socket: ZmqSocket): ZmqMsg = ZmqMsg(ZMsg.recvMsg(socket.handle))
    }

    actual fun copy(): ZmqMsg = ZmqMsg(handle.duplicate())
}
