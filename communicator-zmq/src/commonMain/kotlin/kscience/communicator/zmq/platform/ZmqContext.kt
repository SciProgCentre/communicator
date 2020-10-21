package kscience.communicator.zmq.platform

import kotlinx.io.Closeable

/** Constructor must create a context with its init method */
internal expect class ZmqContext() : Closeable {
    fun createPairSocket(): ZmqSocket
    fun createPubSocket(): ZmqSocket
    fun createSubSocket(): ZmqSocket
    fun createReqSocket(): ZmqSocket
    fun createRepSocket(): ZmqSocket
    fun createDealerSocket(): ZmqSocket
    fun createRouterSocket(): ZmqSocket
    fun createPullSocket(): ZmqSocket
    fun createPushSocket(): ZmqSocket
    fun createXPubSocket(): ZmqSocket
    fun createXSubSocket(): ZmqSocket
    fun createStreamSocket(): ZmqSocket
    override fun close()
}
