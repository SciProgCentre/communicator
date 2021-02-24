package space.kscience.communicator.zmq.client

import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

internal actual fun initClient(client: ZmqTransport) {
    Worker.start().execute(TransferMode.SAFE, client::freeze) { it.start() }
}
