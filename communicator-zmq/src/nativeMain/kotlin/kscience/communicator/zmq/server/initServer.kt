package kscience.communicator.zmq.server

import kotlinx.coroutines.runBlocking
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

internal actual fun initServer(server: ZmqTransportServer) {
    Worker.start().execute(TransferMode.SAFE, server::freeze) { it.start() }
}
