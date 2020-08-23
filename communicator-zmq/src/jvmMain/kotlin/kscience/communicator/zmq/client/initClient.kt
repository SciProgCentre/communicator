package kscience.communicator.zmq.client

import kotlin.concurrent.thread

internal actual fun initClient(state: ClientState) {
    thread(isDaemon = true) { initClientBlocking(state) }
}
