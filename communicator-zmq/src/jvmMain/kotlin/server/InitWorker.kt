package space.kscience.communicator.zmq.server

import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

internal actual fun initWorker(worker: ZmqWorker): Any =
    thread(isDaemon = true, name = "ZmqWorker(${worker.proxy.address})") { runBlocking { worker.start() } }
