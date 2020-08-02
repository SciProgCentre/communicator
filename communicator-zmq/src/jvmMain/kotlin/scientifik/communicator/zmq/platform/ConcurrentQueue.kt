package scientifik.communicator.zmq.platform

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Thread-safe queue
 */
internal actual class ConcurrentQueue<T : Any> actual constructor() {

    private val backupQueue = ConcurrentLinkedQueue<T>()

    /** Adds an element to the end of the queue */
    actual fun add(element: T) {
        backupQueue.add(element)
    }

    /** Removes an element from the start of the queue and returns it. If the queue is empty, returns null. */
    actual fun poll(): T? = backupQueue.poll()

}