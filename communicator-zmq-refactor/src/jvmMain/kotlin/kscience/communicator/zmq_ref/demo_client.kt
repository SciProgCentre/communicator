package kscience.communicator.zmq_ref

import kotlinx.coroutines.runBlocking
import kscience.communicator.zmq_ref.zmq.ZmqContext

internal fun runClient(serverEndpoint: String) = runBlocking {
    val manager = ZmqTransportManager
    val transport = manager.create()
    val payloadFunc = transport.channel(serverEndpoint, "secret")
    val arg = 3
    val res = payloadFunc(byteArrayOf(arg.toByte()))[0].toInt()
    println("CLIENT: arg was $arg, res is $res")
}