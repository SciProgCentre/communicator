package scientifik.communicator.userapi.transport

import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZLoop
import org.zeromq.ZMsg
import scientifik.communicator.userapi.scientifik.communicator.userapi.transport.putUuid
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.*

internal class ClientTransport : Closeable {
    private val context: ZContext = ZContext()
    private val dealer: org.zeromq.ZMQ.Socket = context.createSocket(SocketType.DEALER)
    private val loop: ZLoop = ZLoop(context)

    fun requestEvaluate(functionName: String, blob: ByteArray): Unit = ZMsg().run {
        add(byteArrayOf(T_REQUEST_EVALUATE))

        add(ByteBuffer.allocate(16).run {
            putUuid(UUID.randomUUID())
            array()
        })

        add(blob)
        add(functionName)
        send(dealer)
        Unit
    }

    fun requestCoderID(functionName: String) {
        ZMsg().run {
            add(byteArrayOf(T_REQUEST_CODER_ID))
            add(functionName)
            send(dealer)
        }
    }

    fun revoke(id: UUID): Unit = ZMsg().run {
        add(byteArrayOf(T_REVOCATION))

        add(ByteBuffer.allocate(16).run {
            putUuid(id)
            array()
        })

        send(dealer)
        Unit
    }

    fun receive() {
        loop.
    }

    override fun close() {
        dealer.close()
        context.close()
    }

    private companion object {
        private const val T_REQUEST_EVALUATE: Byte = 11
        private const val T_REQUEST_CODER_ID: Byte = 21
        private const val T_REVOCATION: Byte = 31
    }
}
