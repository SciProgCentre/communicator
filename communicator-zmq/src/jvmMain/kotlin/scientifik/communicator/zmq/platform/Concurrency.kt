package scientifik.communicator.zmq.platform

import kotlin.concurrent.thread

/** Runs an event loop in background thread. Event loop blocks the thread forever. */
actual fun runInBackground(runnable: () -> Unit) {
    thread(isDaemon = true, block = runnable)
}