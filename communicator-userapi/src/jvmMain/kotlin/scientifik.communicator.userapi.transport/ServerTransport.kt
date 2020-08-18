package scientifik.communicator.userapi.transport

import org.zeromq.*
import scientifik.communicator.api.Payload
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.*
import kotlin.concurrent.thread

class ServerTransport(private val endpoint: String) : Closeable {
    private val context: ZContext = ZContext()
    private val router: ZMQ.Socket = context.createSocket(SocketType.ROUTER)
    private val loop: ZLoop = ZLoop(context)
    var functions: MutableMap<String, (Payload) -> Payload?> = hashMapOf()

    fun start() {
        functions = Collections.unmodifiableMap(functions)

        thread {
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
    }

    private fun evaluate(uuid: UUID, arg: Payload, functionName: String): Boolean = ZMsg().apply {
        // TODO
        // Currently doesn't support encoding exceptions.


        val f = functions[functionName] ?: run {
            addLast(byteArrayOf(RESPONSE_FUNCTION_EXCEPTION))
            addLast(ByteBuffer.allocate(16).also { it.putUuid(uuid) }.array())
            addLast("No such function.")
            return@apply
        }

        val res = f(arg) ?: run {
            addLast(byteArrayOf(RESPONSE_FUNCTION_EXCEPTION))
            addLast(ByteBuffer.allocate(16).also { it.putUuid(uuid) }.array())
            addLast("Function did not return.")
            return@apply
        }

        addLast(byteArrayOf(RESPONSE_SUCCESS))
        addLast(res)
        addLast(ByteBuffer.allocate(16).also { it.putUuid(uuid) }.array())
    }.send(router)

    private fun receive() {
        val msg = checkNotNull(ZMsg.recvMsg(router))
        msg.removeFirst()
        val type = msg.first.data.first()
        msg.removeFirst()

        when (type) {
            REQUEST_EVALUATE -> {
                val uuid = ByteBuffer.wrap(msg.first.data).getUuid()
                msg.removeFirst()
                val arg = msg.first.data
                msg.removeFirst()
                val name = msg.first.data.decodeToString()
                evaluate(uuid, arg, name)
            }
            REQUEST_CODER_ID -> TODO()
            REQUEST_REVOCATION -> TODO()
        }
    }

    override fun close() {
        router.close()
        context.close()
    }
}
