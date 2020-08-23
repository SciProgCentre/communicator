package kscience.communicator.zmq.server

import kotlin.concurrent.thread

internal actual fun initServer(server: ZmqTransportServer.ServerState) {
    thread(isDaemon = true) { initServerBlocking(server) }
}
