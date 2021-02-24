package space.kscience.communicator.zmq.server

import kotlin.concurrent.thread

internal actual fun initServer(server: ZmqTransportServer): Any =
    thread(isDaemon = true, name = "ZmqTransportServer(${server.port})") { server.start() }

