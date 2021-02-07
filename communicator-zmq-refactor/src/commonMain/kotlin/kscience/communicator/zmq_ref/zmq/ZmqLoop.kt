package kscience.communicator.zmq_ref.zmq

import kotlinx.io.Closeable

/**
 * after `run` is called, starts a new thread
 * and loops unitl `close` is called
 */
internal expect class ZmqLoop: Closeable {
    fun add(socket: ZmqSocket, handler: () -> Unit)
    fun start()
}