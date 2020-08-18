package scientifik.communicator.userapi.transport

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.zeromq.*
import scientifik.communicator.api.Payload
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

internal class ClientTransport(private val endpoint: String) : Closeable {
    private val context: ZContext = ZContext()
    private val dealer: ZMQ.Socket = context.createSocket(SocketType.DEALER)
    private val loop: ZLoop = ZLoop(context)

    data class Expectation(
            val possibleCodes: ByteArray,
            val predicate: (ZMsg) -> Boolean,
            val callback: (ZMsg, Byte) -> Unit
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Expectation

            if (!possibleCodes.contentEquals(other.possibleCodes)) return false
            if (predicate != other.predicate) return false
            if (callback != other.callback) return false

            return true
        }

        override fun hashCode(): Int {
            var result = possibleCodes.contentHashCode()
            result = 31 * result + predicate.hashCode()
            result = 31 * result + callback.hashCode()
            return result
        }
    }

    private val expectations: MutableList<Expectation> = Collections.synchronizedList(mutableListOf())

    fun start() {
        thread {
            dealer.connect(endpoint)

            loop.addPoller(
                    ZMQ.PollItem(dealer, ZMQ.Poller.POLLIN),
                    { _, _, _ ->
                        receive()
                        0
                    },
                    null
            )

            loop.start()
        }
    }

    private fun requestEvaluate(functionName: String, uuid: UUID, blob: ByteArray): Unit = ZMsg().run {
        add(byteArrayOf(REQUEST_EVALUATE))

        add(ByteBuffer.allocate(16).run {
            putUuid(uuid)
            array()
        })

        add(blob)
        add(functionName)
        send(dealer)
        Unit
    }

    private fun requestCoderID(functionName: String) {
        ZMsg().run {
            add(byteArrayOf(REQUEST_CODER_ID))
            add(functionName)
            send(dealer)
        }
    }

    private fun revoke(id: UUID): Unit = ZMsg().run {
        add(byteArrayOf(REQUEST_REVOCATION))

        add(ByteBuffer.allocate(16).run {
            putUuid(id)
            array()
        })

        send(dealer)
        Unit
    }

    private fun receive() {
        println("Client received message")
        val msg = checkNotNull(ZMsg.recvMsg(dealer))
        println(buildString { msg.dump(this) })
        val type = msg.first.data[0]
        msg.removeFirst()
        val it = expectations.asReversed().listIterator()

        while (it.hasNext()) {
            val (t, p, c) = it.next()

            if (type in t && p(msg)) {
                c(msg, type)
                it.remove()
            }
        }
    }

    fun evaluateAsync(functionName: String, blob: Payload): Deferred<Payload> {
        val uuid = UUID.randomUUID()
        requireNotNull(uuid)
        requestEvaluate(functionName, uuid, blob)
        val deferred = CompletableDeferred<ByteArray>()

        expectations.add(Expectation(
                byteArrayOf(11, 12, 13, 14),
                { ByteBuffer.wrap(it.first.data).getUuid() == uuid }) { msg, type ->
            revoke(uuid)
            msg.removeFirst()

            when (type) {
                RESPONSE_SUCCESS -> deferred.complete(msg.first.data)
                else -> error(msg.first.data.decodeToString())
            }
        })

        return deferred
    }

    fun coderIDAsync(functionName: String): Deferred<UUID?> {
        requestCoderID(functionName)
        val deferred = CompletableDeferred<UUID?>()

        expectations.add(Expectation(
                byteArrayOf(21, 22),
                { it.first.data.decodeToString() == functionName }) { msg, type ->
            msg.removeFirst()

            when (type) {
                RESPONSE_FUNCTION_SUPPORTED -> deferred.complete(ByteBuffer.wrap(msg.first.data).getUuid())
                RESPONSE_FUNCTION_UNSUPPORTED -> deferred.complete(null)
                else -> error("Illegal code $type")
            }
        })

        return deferred
    }

    override fun close() {
        dealer.close()
        context.close()
    }
}
