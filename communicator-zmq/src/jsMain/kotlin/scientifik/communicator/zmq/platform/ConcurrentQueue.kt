package scientifik.communicator.zmq.platform

/**
 * Thread-safe queue
 */
internal actual class ConcurrentQueue<T> actual constructor() : MutableCollection<T> {
    override val size: Int
        get() = TODO()

    /** Adds an element to the end of the queue */
    override fun add(element: T): Boolean = TODO()

    /** Removes an element from the start of the queue and returns it. If the queue is empty, returns null. */
    actual fun poll(): T? = TODO()

    override fun contains(element: T): Boolean = TODO()
    override fun containsAll(elements: Collection<T>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun addAll(elements: Collection<T>): Boolean = TODO()
    override fun clear(): Unit = TODO()
    override fun iterator(): MutableIterator<T> = TODO()
    override fun remove(element: T): Boolean = TODO()
    override fun removeAll(elements: Collection<T>): Boolean = TODO()
    override fun retainAll(elements: Collection<T>): Boolean = TODO()
}