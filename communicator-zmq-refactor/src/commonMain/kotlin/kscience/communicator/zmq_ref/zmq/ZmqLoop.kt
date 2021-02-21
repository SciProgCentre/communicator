package kscience.communicator.zmq_ref.zmq

import kotlinx.io.Closeable
import kscience.communicator.zmq_ref.runInBackground


/**
 * after `run` is called, blocks thread. NOT thread-safe
 */
internal expect class ZmqLoop {
    fun add(socket: ZmqSocket, handler: () -> Unit)
    fun start()
    fun stop()
}

/**
 * Wraps loop to simplify a common usecase:
 * it creates a loop, creates a way to stop it safely,
 * lets user init it and starts it in a background
 * expects user to call 'start()' on the loop.
 */
internal class ZmqLoopJob(
    private val ctx: ZmqContext,
    private val endpoint: String): Closeable {

    private var started = false

    // NOT thread-safe
    inline fun start(crossinline loopInit: (ZmqLoop) -> Unit) {
        if (started) {
            return
        }
        started = true
        runInBackground {
            val loop = ctx.createLoop()
            val closeSocket = ctx.createSocket(ZmqSocketType.PULL)
            closeSocket.bind(endpoint)

            loop.add(closeSocket) {
                loop.stop()
            }
            loopInit(loop)
        }
    }

    // this is thread-safe
    override fun close() {
        val stopper = ctx.createSocket(ZmqSocketType.PUSH)
        stopper.connect(endpoint)
        val message = ZmqMessage().apply { add("close") }
        //TODO: should this be non-blocking?
        stopper.send(message)
    }

}