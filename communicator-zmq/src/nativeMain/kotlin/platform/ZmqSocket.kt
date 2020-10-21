package space.kscience.communicator.zmq.platform

import io.ktor.utils.io.core.Closeable
import kotlinx.cinterop.*
import org.zeromq.czmq.*

internal actual class ZmqSocket internal constructor(val handle: CPointer<zsock_t>) : Closeable {
    init {
        require(zsock_is(handle)) { "Provided pointer $handle doesn't point to zsock_t." }
    }

    actual fun connect(zmqAddress: String): Unit =
        zsock_connect(handle, zmqAddress).checkReturnState("zsock_connect")

    actual fun bind(zmqAddress: String): Unit =
        zsock_bind(handle, zmqAddress).checkReturnState("zsock_bind")

    actual fun setIdentity(identity: ByteArray): Unit = zsock_set_identity(handle, identity.decodeToString())

    actual override fun close(): Unit = memScoped {
        if (!zsock_is(handle)) return@memScoped
        val cpv = alloc<CPointerVar<zsock_t>>()
        cpv.value = handle
        val a = allocPointerTo<CPointerVar<zsock_t>>()
        a.pointed = cpv
        zsock_destroy(a.value)
    }

    actual fun recv(): ByteArray = ZmqFrame.recvFrame(this).data
}
