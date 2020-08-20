package kscience.communicator.zmq.platform

/* On NodeJS, volatileJob should probably be launched in nodejs worker */
internal actual inline fun <T1, T2> runInBackground(supplier: () -> T1, noinline volatileJob: (T1) -> T2) {
    TODO()
}
