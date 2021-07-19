package space.kscience.communicator.prettyapi

import kotlinx.coroutines.runBlocking
import space.kscience.communicator.api.ClientEndpoint
import space.kscience.communicator.zmq.zmq
import java.io.IOException
import java.net.ServerSocket
import kotlin.test.fail

internal inline fun runTest(crossinline block: suspend CommunicatorContext.() -> Unit) {
    val port: Int = try {
        ServerSocket(0).use { socket -> socket.localPort }
    } catch (e: IOException) {
        fail("Can't find a free port.", e)
    }

    communicator(ClientEndpoint("ZMQ", "localhost", port)) {
        defaultTransport { zmq() }
        runBlocking { block() }
    }
}
