package space.kscience.communicator.zmq.util

internal expect fun <T> runAsync(receiver: T, action: suspend (T) -> Unit): Any
