package kscience.communicator.zmq.platform

import org.zeromq.czmq.*
import kotlinx.cinterop.*
import kotlinx.io.Closeable

internal actual class ZmqSocket internal constructor(val backendSocket: CPointer<zsock_t>) : Closeable {
    init {
        require(zsock_is(backendSocket)) { "Provided pointer $backendSocket doesn't point to zsock_t." }
    }

    actual fun connect(zmqAddress: String): Unit =
        zsock_connect(backendSocket, zmqAddress).checkZeroMQCode("zsock_connect")

    actual fun bind(zmqAddress: String): Unit =
        zsock_bind(backendSocket, zmqAddress).checkZeroMQCode("zsock_bind")

    actual fun setIdentity(identity: ByteArray): Unit = zsock_set_identity(backendSocket, identity.decodeToString())

    override fun close(): Unit = memScoped {
        if (!zsock_is(backendSocket)) return@memScoped
        val cpv: CPointerVar<zsock_t> = alloc()
        cpv.value = backendSocket
        val a = allocPointerTo<CPointerVar<zsock_t>>()
        a.pointed = cpv
        zsock_destroy(a.value)
    }

    actual fun recv(): ByteArray {
        val frame = zframe_recv(backendSocket)
        return checkNotNull(zframe_data(frame)).readBytes(zframe_size(frame).toInt())
    }
}
