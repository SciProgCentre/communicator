package scientifik.communicator.zmq.platform

/**
 * Thread-safe queue
 */
internal actual class ConcurrentQueue<T : Any> actual constructor() {
    /** Adds an element to the end of the queue */
    actual fun add(element: T) {
    }

    /** Removes an element from the start of the queue and returns it. If the queue is empty, returns null. */
    actual fun poll(): T? {
        TODO("Not yet implemented")
    }

}