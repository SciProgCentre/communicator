package space.kscience.communicator.zmq.client

import kotlin.concurrent.thread

internal actual fun initClient(client: ZmqTransport): Any =
    thread(isDaemon = true, name = "ZmqTransport(${client.identityHash})", block = client::start)
