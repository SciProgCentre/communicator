package kscience.communicator.zmq.server

import kotlin.concurrent.thread

internal actual fun initWorker(state: ZmqWorker) {
    thread(isDaemon = true) { initWorkerBlocking(state) }
}
