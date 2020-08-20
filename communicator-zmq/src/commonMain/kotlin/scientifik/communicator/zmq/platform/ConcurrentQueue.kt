package scientifik.communicator.zmq.platform

/**
 * Thread-safe queue.
 * Instances of this class will be frozen on K/N, so choose implementation wisely.
 */
internal expect class ConcurrentQueue<T>() : MutableCollection<T> {
    /** Removes an element from the start of the queue and returns it. If the queue is empty, returns null. */
    fun poll(): T?
}
