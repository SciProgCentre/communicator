package kscience.communicator.zmq.client

import kotlin.concurrent.thread

internal actual fun initClient(client: ZmqTransport) {
    thread(isDaemon = true, name = client.toString(), block = client::start)
}
