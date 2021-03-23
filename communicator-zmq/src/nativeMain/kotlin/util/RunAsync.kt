package space.kscience.communicator.zmq.util

import kotlinx.coroutines.runBlocking
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

internal actual fun <T> runAsync(receiver: T, action: suspend (T) -> Unit): Any = Worker
    .start()
    .execute(TransferMode.SAFE, { receiver.freeze() to action.freeze() }) { (a, b) -> runBlocking { b(a) } }
