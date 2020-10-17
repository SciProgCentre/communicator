package kscience.communicator.zmq.server

import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

internal actual fun initWorker(worker: ZmqWorker) {
    Worker.start().execute(TransferMode.SAFE, worker::freeze, ::initWorkerBlocking)
}
