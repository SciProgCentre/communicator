package kscience.communicator.zmq_ref

import kscience.communicator.api_ref.Payload
import kscience.communicator.api_ref.PayloadFunction
import kscience.communicator.zmq_ref.zmq.ZmqContext
import java.lang.Thread.sleep

fun secret(a: Int): Int {
    return a * 2
}


internal fun runServer(ctx: ZmqContext, endpoint: String="test_server") {
    val server = ZMQTransportServer(ctx, endpoint, "anything")
    val payloadFunction: PayloadFunction = {p: Payload ->
        val arg = p[0].toInt()
        val res = secret(arg)
        println("SERVER: arg was $arg, result is $res")
        byteArrayOf(res.toByte())
    }
    server.register("secret", payloadFunction)
    while(true) {
        sleep(100)
        println("SERVER: thread alive")
    }
}