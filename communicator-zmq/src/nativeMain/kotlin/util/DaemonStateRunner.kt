package space.kscience.communicator.zmq.util

import co.touchlab.stately.isolate.BackgroundStateRunner
import co.touchlab.stately.isolate.StateRunner

internal actual class DaemonStateRunner : StateRunner {
    private val background = BackgroundStateRunner()
    actual override fun <R> stateRun(block: () -> R): R = background.stateRun(block)
    actual override fun stop() = background.stop()
}
