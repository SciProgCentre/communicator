package kscience.communicator.zmq.client

import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

internal actual fun initClient(state: ClientState) {
    Worker.start().execute(TransferMode.SAFE, { state.freeze() }, ::initClientBlocking)
}
