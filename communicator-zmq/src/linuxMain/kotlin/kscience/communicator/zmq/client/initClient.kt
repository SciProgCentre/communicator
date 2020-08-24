package kscience.communicator.zmq.client

import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

internal actual fun initClient(state: ClientState) {
//    GlobalScope.launch { initClientBlocking(state/*.freeze()*/) }
    Worker.start().execute(TransferMode.SAFE, state::freeze, ::initClientBlocking)
}
