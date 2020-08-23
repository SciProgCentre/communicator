package kscience.communicator.zmq.server

import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

internal actual fun initServer(server: ZmqTransportServer.ServerState) {
    Worker.start().execute(TransferMode.SAFE, { server.freeze() }, ::initServerBlocking)
}
