package scientifik.communicator.zmq.platform

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Thread-safe queue
 */
internal actual class ConcurrentQueue<T> private constructor(private val backendQueue: Queue<T>) :
    MutableCollection<T> by backendQueue {
    actual constructor() : this(ConcurrentLinkedQueue())

    /** Removes an element from the start of the queue and returns it. If the queue is empty, returns null. */
    actual fun poll(): T? = backendQueue.poll()
}
