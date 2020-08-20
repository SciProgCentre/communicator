package kscience.communicator.zmq.platform

/* On NodeJS, volatileJob should probably be launched in nodejs worker */
internal actual fun <T1, T2> runInBackground(supplier: () -> T1, volatileJob: (T1) -> T2) {
    TODO()
}
