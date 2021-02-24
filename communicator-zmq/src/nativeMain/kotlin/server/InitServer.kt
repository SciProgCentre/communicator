package space.kscience.communicator.zmq.server

import kotlinx.coroutines.runBlocking
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

internal actual fun initServer(server: ZmqTransportServer): Any =
    Worker.start().execute(TransferMode.SAFE, server::freeze) { runBlocking { it.start() } }
