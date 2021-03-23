package space.kscience.communicator.zmq.util

import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

internal actual fun <T> runAsync(receiver: T, action: suspend (T) -> Unit): Any =
    thread(isDaemon = true, name = receiver.toString()) {
        runBlocking { action(receiver) }
    }
