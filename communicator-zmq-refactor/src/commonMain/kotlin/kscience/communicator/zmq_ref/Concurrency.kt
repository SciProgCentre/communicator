package kscience.communicator.zmq_ref


internal expect fun runInBackground(job: ()->Unit)

/**
 * Should generate different ids each time and be thread-safe
 */
expect class IdGenerator() {
    fun getNext(): String
}