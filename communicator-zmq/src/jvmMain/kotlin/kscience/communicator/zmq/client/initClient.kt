package kscience.communicator.zmq.client

import kotlin.concurrent.thread

internal actual fun initClient(client: Client) {
    thread(isDaemon = true) { initClientBlocking(client) }
}
