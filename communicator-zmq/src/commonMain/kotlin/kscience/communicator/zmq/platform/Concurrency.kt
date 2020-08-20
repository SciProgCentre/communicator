package kscience.communicator.zmq.platform


/**
 * Runs an event loop in background thread (worker).
 * Event loop blocks the thread forever.
 * To simplify interop with K/N, semantics are similar to Worker.execute()
 * [supplier] is executed in the thread that called the function,
 * then its result should be transferred to another thread,
 * where [volatileJob] should be called.
 * To simplify interop with K/N (and gain some thread safety on other platforms),
 * [volatileJob] does not capture any state.
 */
internal expect fun <T1, T2> runInBackground(
    supplier: () -> T1,
    volatileJob: (T1) -> T2
)
