package kscience.communicator.zmq.server

import kotlin.concurrent.thread

internal actual fun initWorker(worker: ZmqWorker) {
    thread(isDaemon = true, name = worker.toString()) { initWorkerBlocking(worker) }
}
