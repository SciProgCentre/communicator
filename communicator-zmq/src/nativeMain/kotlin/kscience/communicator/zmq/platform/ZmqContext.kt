package kscience.communicator.zmq.platform

import co.touchlab.stately.collections.IsoMutableList
import kotlinx.io.Closeable
import org.zeromq.czmq.*

internal actual class ZmqContext actual constructor() : Closeable {
    private val sockets: IsoMutableList<ZmqSocket> = IsoMutableList()

    init {
        zsys_init()
        zsys_set_logstream(null)
    }

    actual fun createRouterSocket(): ZmqSocket =
        ZmqSocket(checkNotNull(zsock_new_router(null)) { "zsock_new_router returned null." }).also { sockets += it }

    actual fun createDealerSocket(): ZmqSocket =
        ZmqSocket(checkNotNull(zsock_new_dealer(null)) { "zsock_new_dealer returned null." }).also { sockets += it }

    actual override fun close(): Unit = sockets.forEach(ZmqSocket::close)

    actual fun createPairSocket(): ZmqSocket =
        ZmqSocket(checkNotNull(zsock_new_pair(null)) { "zsock_new_pair returned null." }).also { sockets += it }

    actual fun createPubSocket(): ZmqSocket =
        ZmqSocket(checkNotNull(zsock_new_pub(null)) { "zsock_new_pub returned null." }).also { sockets += it }

    actual fun createSubSocket(): ZmqSocket =
        ZmqSocket(checkNotNull(zsock_new_sub(null, null)) { "zsock_new_sub returned null." }).also { sockets += it }

    actual fun createReqSocket(): ZmqSocket =
        ZmqSocket(checkNotNull(zsock_new_req(null)) { "zsock_new_req returned null." }).also { sockets += it }

    actual fun createRepSocket(): ZmqSocket =
        ZmqSocket(checkNotNull(zsock_new_rep(null)) { "zsock_new_rep returned null." }).also { sockets += it }

    actual fun createPullSocket(): ZmqSocket =
        ZmqSocket(checkNotNull(zsock_new_pull(null)) { "zsock_new_pull returned null." }).also { sockets += it }

    actual fun createPushSocket(): ZmqSocket =
        ZmqSocket(checkNotNull(zsock_new_push(null)) { "zsock_new_push returned null." }).also { sockets += it }

    actual fun createXPubSocket(): ZmqSocket =
        ZmqSocket(checkNotNull(zsock_new_xpub(null)) { "zsock_new_xpub returned null." }).also { sockets += it }

    actual fun createXSubSocket(): ZmqSocket =
        ZmqSocket(checkNotNull(zsock_new_xsub(null)) { "zsock_new_xsub returned null." }).also { sockets += it }

    actual fun createStreamSocket(): ZmqSocket =
        ZmqSocket(checkNotNull(zsock_new_stream(null)) { "zsock_new_stream returned null." }).also { sockets += it }
}
