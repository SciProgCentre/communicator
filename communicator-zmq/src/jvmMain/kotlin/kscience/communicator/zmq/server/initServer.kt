package kscience.communicator.zmq.server

import kotlin.concurrent.thread
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal actual fun initServer(server: ZmqTransportServer.ServerState) {
    thread(isDaemon = true) { initServerBlocking(server) }
}

internal actual inline fun runBlockingIfKotlinNative(crossinline action: () -> Any) {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    action()
}
