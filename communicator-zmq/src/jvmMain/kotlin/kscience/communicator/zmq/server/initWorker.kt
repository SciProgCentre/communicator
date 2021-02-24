package kscience.communicator.zmq.server

import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

internal actual fun initWorker(worker: ZmqWorker) {
    thread(isDaemon = true, name = "ZmqWorker(${worker.proxy.address})") { runBlocking { worker.start() } }
}
