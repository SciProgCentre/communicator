package kscience.communicator.zmq_ref

import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

internal actual fun runInBackground(job: ()->Unit){
    thread(block=job)
}

actual class IdGenerator actual constructor() {
    private val currentId = AtomicInteger()

    actual fun getNext(): String {
        val id = currentId.getAndIncrement()
        return id.toString()
    }

}