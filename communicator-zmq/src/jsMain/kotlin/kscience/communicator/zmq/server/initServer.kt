package kscience.communicator.zmq.server

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal actual fun initServer(server: ZmqTransportServer.ServerState): Unit = TODO()

internal actual inline fun runBlockingIfKotlinNative(crossinline action: () -> Any) {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    action()
}
