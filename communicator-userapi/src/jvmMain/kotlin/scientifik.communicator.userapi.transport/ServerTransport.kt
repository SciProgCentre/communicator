package scientifik.communicator.userapi.transport

import org.zeromq.*
import scientifik.communicator.api.Payload
import java.io.Closeable

class ServerTransport(endpoint: String) : Closeable {
    private val context: ZContext = ZContext()
    private val router: ZMQ.Socket = context.createSocket(SocketType.ROUTER)
    private val loop: ZLoop = ZLoop(context)
    val functions: MutableMap<String, (Payload) -> Payload?> = hashMapOf()

    init {
        router.bind(endpoint)

        loop.addPoller(
            ZMQ.PollItem(router, ZMQ.Poller.POLLIN),
            { _, _, _ ->
                receive()
                0
            },
            null
        )

        loop.start()
    }

    fun evaluate(arg: Payload, functionName: String) {
        val msg = ZMsg()

        val f = functions[functionName] ?: run {
            msg.push("No such function.")
            msg.send(router)
            return
        }

        val res = f(arg)
        msg.push(res)
        msg.send(router)
    }

    private fun receive() {
        val msg = checkNotNull(ZMsg.recvMsg(router))
        val type = msg.first.data.first()
        msg.pop()

        when (type) {
            REQUEST_EVALUATE -> {
                val arg = msg.first.data
                msg.pop()
                val name = msg.first.data.decodeToString()
                evaluate(arg, name)
            }
            REQUEST_CODER_ID -> TODO()
            REQUEST_REVOCATION -> {
            }
        }
    }

    override fun close() {
        router.close()
        context.close()
    }
}
