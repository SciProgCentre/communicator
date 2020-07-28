package scientifik.communicator.zmq.platform

/** Runs an event loop in background thread. Event loop blocks the thread forever. */
expect fun runInBackground(runnable: () -> Unit)