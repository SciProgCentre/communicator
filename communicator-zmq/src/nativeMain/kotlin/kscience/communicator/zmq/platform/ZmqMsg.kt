package kscience.communicator.zmq.platform

import kotlinx.cinterop.*
import kotlinx.io.Closeable
import org.zeromq.czmq.*

internal actual class ZmqMsg internal constructor(val handle: CPointer<zmsg_t>) : Closeable,
    AbstractMutableCollection<ZmqFrame>(),
    MutableCollection<ZmqFrame> {
    actual constructor() : this(checkNotNull(zmsg_new()) { "zmsg_new returned null." })

    init {
        require(zmsg_is(handle)) { "Provided pointer $handle doesn't point to zmsg_t." }
    }

    override val size: Int
        get() = zmsg_size(handle).toInt()

    actual fun pop(): ZmqFrame =
        ZmqFrame(checkNotNull(zmsg_pop(handle)) { "The zmsg is empty, or zmsg_pop returned null." })

    actual fun send(socket: ZmqSocket): Unit = memScoped {
        val cpv: CPointerVar<zmsg_t> = alloc()
        cpv.value = handle
        val a = allocPointerTo<CPointerVar<zmsg_t>>()
        a.pointed = cpv
        zmsg_send(a.value, socket.handle).checkReturnState("zmsg_send")
    }

    actual override fun close(): Unit = memScoped {
        if (!zmsg_is(handle)) return@memScoped
        val cpv = alloc<CPointerVar<zmsg_t>>()
        cpv.value = handle
        val a = allocPointerTo<CPointerVar<zmsg_t>>()
        a.pointed = cpv
        zmsg_destroy(a.value)
    }

    actual fun add(data: ByteArray): Boolean {
        zmsg_addmem(handle, data.toCValues(), data.size.toULong()).checkReturnState("zmsg_addmem")
        return true
    }

    actual companion object {
        actual fun recvMsg(socket: ZmqSocket): ZmqMsg =
            ZmqMsg(checkNotNull(zmsg_recv(socket.handle)) { "zmsg_recv returned null." })
    }

    override fun add(element: ZmqFrame): Boolean {
        zmsg_add(handle, element.handle).checkReturnState("zmsg_add")
        return true
    }

    actual fun copy(): ZmqMsg = ZmqMsg(checkNotNull(zmsg_dup(handle)) { "zmsg_dup returned null." })

    override fun iterator(): MutableIterator<ZmqFrame> {
        val copy = copy()

        return object : MutableIterator<ZmqFrame> {
            private var current: CPointer<zframe_t>? = null
            private var finished = false

            override fun hasNext(): Boolean {
                if (finished) return false

                return if (zmsg_size(copy.handle) == 0uL) {
                    finished = true
                    copy.close()
                    false
                } else
                    true
            }

            override fun next(): ZmqFrame {
                if (!hasNext()) throw NoSuchElementException()
                current = zmsg_pop(copy.handle)
                return ZmqFrame(checkNotNull(current) { "Current frame is null, but the size of zmsg is more than 0." })
            }

            override fun remove(): Unit = zmsg_remove(handle, current)
        }
    }
}
