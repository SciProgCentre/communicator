package scientifik.communicator.userapi.transport

import org.zeromq.*
import scientifik.communicator.api.Payload
import scientifik.communicator.api.log
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

    private fun evaluate(clientIdentity: ByteArray, uuid: UUID, arg: Payload, functionName: String): Boolean =
        ZMsg().apply {
            // TODO
            // Currently doesn't support encoding exceptions.

            add(clientIdentity)

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
            addLast(ByteBuffer.allocate(16).also { it.putUuid(uuid) }.array())
            addLast(res)
        }.send(router)

    private fun receive() {
        log("$this received message")
        val msg = checkNotNull(ZMsg.recvMsg(router))
        log(buildString { msg.dump(this) })
        val clientIdentity = msg.first.data
        msg.removeFirst()
        val type = msg.first.data.first()
        msg.removeFirst()

        when (type) {
            REQUEST_EVALUATE -> {
                log("REQUEST_EVALUATE")
                val uuid = ByteBuffer.wrap(msg.first.data).getUuid()
                msg.removeFirst()
                val arg = checkNotNull(msg.first.data)
                log("argBytes = ${arg.contentToString()}")
                msg.removeFirst()
                val name = msg.first.data.decodeToString()
                evaluate(clientIdentity, uuid, arg, name)
            }

            // TODO
            REQUEST_CODER_ID -> {}

            // TODO
            REQUEST_REVOCATION -> {}
        }
    }

    override fun close() {
        router.close()
        context.close()
    }
}
