package scientifik.communicator.zmq.proxy

import org.zeromq.*
import scientifik.communicator.zmq.platform.UniqueID

/**
 * Starts a proxy that listens to the given port.
 * This method blocks the thread and is recommended to be called from a separate program.
 * */
fun startProxy(port: Int) {
    ZmqProxy(port).start()
}

internal class Worker(
        val identity: ByteArray,
        val functions: MutableList<String>,
        var lastHeartbeatTime: Long
)

internal class ZmqProxy(private val port: Int) {

    internal val workers = mutableListOf<Worker>()
    internal val functionSchemes = hashMapOf<String, Pair<String, String>>()
    internal val workersByFunction = hashMapOf<String, MutableList<Worker>>()
    // query id -> client identity
    internal val receivedQueries = hashMapOf<UniqueID, ByteArray>()
    internal val sentQueries = hashSetOf<UniqueID>()
    internal val sentResults = hashSetOf<UniqueID>()

    fun start() {
        val ctx = ZContext()
        val frontend = ctx.createSocket(SocketType.ROUTER)
        val backend = ctx.createSocket(SocketType.ROUTER)
        frontend.bind("tcp://*:$port")
        backend.bind("tcp://*:${port + 1}")
        val poller = ctx.createPoller(2)
        poller.register(frontend)
        poller.register(backend)
        while (true) {
            if (poller.poll() < 0) continue
            if (poller.pollin(0)) handleFrontend(frontend, backend)
            if (poller.pollin(1)) handleBackend(frontend, backend)
        }
    }

}
