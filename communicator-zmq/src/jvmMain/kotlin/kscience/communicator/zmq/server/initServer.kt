package kscience.communicator.zmq.server

import kotlin.concurrent.thread
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal actual fun initServer(server: ZmqTransportServer) {
    thread(isDaemon = true, name = "ZmqTransportServer(${server.port})") { server.start() }
}

internal actual inline fun runBlockingIfKotlinNative(crossinline action: () -> Any) {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    action()
}
