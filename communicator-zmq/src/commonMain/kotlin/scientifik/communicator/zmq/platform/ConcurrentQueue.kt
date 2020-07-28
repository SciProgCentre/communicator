package scientifik.communicator.zmq.platform

/**
 * Thread-safe queue
 */
expect class ConcurrentQueue<T : Any>() {

    /** Adds an element to the end of the queue */
    fun add(element: T)

    /** Removes an element from the start of the queue and returns it. If the queue is empty, returns null. */
    fun poll(): T?

}