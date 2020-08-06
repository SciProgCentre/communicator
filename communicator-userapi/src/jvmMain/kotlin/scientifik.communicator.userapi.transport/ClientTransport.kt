package scientifik.communicator.userapi.transport

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.zeromq.*
import scientifik.communicator.api.Payload
import scientifik.communicator.userapi.scientifik.communicator.userapi.transport.getUuid
import scientifik.communicator.userapi.scientifik.communicator.userapi.transport.putUuid
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.*

internal class ClientTransport : Closeable {
    private val context: ZContext = ZContext()
    private val dealer: ZMQ.Socket = context.createSocket(SocketType.DEALER)
    private val loop: ZLoop = ZLoop(context)

    class Expectation(
        private val possibleCodes: ByteArray,
        private val predicate: (ZMsg) -> Boolean,
        private val callback: (ZMsg, Byte) -> Unit
    ) {
        operator fun component1(): ByteArray = possibleCodes
        operator fun component2(): (ZMsg) -> Boolean = predicate
        operator fun component3(): (ZMsg, Byte) -> Unit = callback
    }

    private val expectations: MutableList<Expectation> =
        mutableListOf()

    init {
        val pollItem = ZMQ.PollItem(dealer, ZMQ.Poller.POLLIN)

        loop.addPoller(pollItem, { _, _, _ ->
            receive()
            0
        }, null)

        loop.start()
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
        val msg = ZMsg.recvMsg(dealer)
        val type = msg.first.data[0]
        msg.removeFirst()

        val ri = expectations.asReversed().listIterator()

        while (ri.hasNext()) {
            val (t, p, c) = ri.next()

            if (type in t && p(msg)) {
                c(msg, type)
                ri.remove()
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

    private companion object {
        private const val REQUEST_EVALUATE: Byte = 11
        private const val REQUEST_CODER_ID: Byte = 21
        private const val REQUEST_REVOCATION: Byte = 31
        private const val RESPONSE_SUCCESS: Byte = 11
        private const val RESPONSE_FUNCTION_EXCEPTION: Byte = 12
        private const val RESPONSE_DECODING_EXCEPTION: Byte = 13
        private const val RESPONSE_ENCODING_EXCEPTION: Byte = 14
        private const val RESPONSE_FUNCTION_SUPPORTED: Byte = 21
        private const val RESPONSE_FUNCTION_UNSUPPORTED: Byte = 22
        private const val RESPONSE_RECEIVED: Byte = 31
    }
}
