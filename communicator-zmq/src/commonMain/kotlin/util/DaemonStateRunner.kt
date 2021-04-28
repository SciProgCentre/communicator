package space.kscience.communicator.zmq.util

import co.touchlab.stately.isolate.StateRunner

internal expect class DaemonStateRunner constructor() : StateRunner {
    override fun <R> stateRun(block: () -> R): R
    override fun stop()
}
