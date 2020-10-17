package kscience.communicator.zmq.platform

import kotlinx.cinterop.*
import kotlinx.io.Closeable
import org.zeromq.czmq.*

internal actual class ZmqMsg internal constructor(val backendMsg: CPointer<zmsg_t>) : Closeable,
    AbstractMutableCollection<ZmqFrame>(),
    MutableCollection<ZmqFrame> {
    actual constructor() : this(checkNotNull(zmsg_new()))

    init {
        require(zmsg_is(backendMsg)) { "Provided pointer $backendMsg doesn't point to zmsg_t." }
    }

    override val size: Int
        get() = zmsg_size(backendMsg).toInt()

    actual fun pop(): ZmqFrame = ZmqFrame(checkNotNull(zmsg_pop(backendMsg)))

    actual fun send(socket: ZmqSocket): Unit = memScoped {
        val cpv: CPointerVar<zmsg_t> = alloc()
        cpv.value = backendMsg
        val a = allocPointerTo<CPointerVar<zmsg_t>>()
        a.pointed = cpv
        zmsg_send(a.value, socket.backendSocket).checkZeroMQCode("zmsg_send")
    }

    override fun close(): Unit = memScoped {
        if (!zmsg_is(backendMsg)) return@memScoped
        val cpv: CPointerVar<zmsg_t> = alloc()
        cpv.value = backendMsg
        val a = allocPointerTo<CPointerVar<zmsg_t>>()
        a.pointed = cpv
        zmsg_destroy(a.value)
    }

    actual fun add(data: ByteArray): Boolean {
        zmsg_addmem(backendMsg, data.toCValues(), data.size.toULong()).checkZeroMQCode("zmsg_addmem")
        return true
    }

    actual companion object {
        actual fun recvMsg(socket: ZmqSocket): ZmqMsg = ZmqMsg(checkNotNull(zmsg_recv(socket.backendSocket)))
    }

    override fun add(element: ZmqFrame): Boolean {
        zmsg_add(backendMsg, element.backendFrame).checkZeroMQCode("zmsg_add")
        return true
    }

    override fun iterator(): MutableIterator<ZmqFrame> {
        check(zmsg_is(backendMsg)) { "Not a message!" }
        val copy = checkNotNull(zmsg_dup(backendMsg))

        return object : MutableIterator<ZmqFrame> {
            var current: CPointer<zframe_t>? = null

            override fun hasNext(): Boolean = zmsg_size(copy) != 0uL

            override fun next(): ZmqFrame {
                if (!hasNext()) throw NoSuchElementException()
                current = zmsg_pop(copy)
                return ZmqFrame(checkNotNull(current))
            }

            override fun remove(): Unit = zmsg_remove(backendMsg, current)
        }
    }
}
