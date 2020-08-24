package kscience.communicator.zmq.server

import kotlinx.coroutines.runBlocking
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

internal actual fun initServer(server: ZmqTransportServer.ServerState) {
//    GlobalScope.launch { initServerBlocking(server) }
    Worker.start().execute(TransferMode.SAFE, server::freeze, ::initServerBlocking)
}

internal actual inline fun runBlockingIfKotlinNative(crossinline action: () -> Any) {
    contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
    runBlocking { action() }
}
