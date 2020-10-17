package kscience.communicator.zmq.client

import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

internal actual fun initClient(client: Client) {
    Worker.start().execute(TransferMode.SAFE, client::freeze, ::initClientBlocking)
}
