package kscience.communicator.zmq.server

import kotlin.concurrent.thread
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal actual fun initServer(server: ZmqTransportServer) {
    thread(isDaemon = true, name = "ZmqTransportServer(${server.port})") { server.start() }
}
