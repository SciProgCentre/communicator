package scientifik.communicator.zmq.platform

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/** Runs an event loop in background thread. Event loop blocks the thread forever. */
actual fun runInBackground(runnable: () -> Unit) {
    GlobalScope.launch { runnable() }
}