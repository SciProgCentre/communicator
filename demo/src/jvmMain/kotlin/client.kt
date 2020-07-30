import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import scientifik.communicator.api.*
import scientifik.communicator.api.userapi.placeOrder
import scientifik.communicator.zmq.client.ZMQTransport
import scientifik.communicator.factories.DefaultFunctionServer
import scientifik.communicator.factories.DefaultTransportFactory


class client(transportFactory: TransportFactory = DefaultTransportFactory(),
             private val functionalServer: FunctionServer = DefaultFunctionServer(
                     listOf(Endpoint("ZMQ", "127.0.0.1:3333"))),
             private val contractorAddress: String = "127.0.0.1:4444",
             private val transport: Transport = transportFactory["ZMQ"]!!
) {
    private val intCoder = IntCoder()
    private val stringCoder = StringCoder()
    private var lambdaId = 0
    object remoteFunctions{
        val call = "call"
        val callUpTo = "callUpTo"
    }
    private fun getNextId(): String {
        return lambdaId++.toString()
    }

    fun call(f: (Int) -> String, a: Int): String {
        val wrapped: suspend (Int) -> String = {f(it)}
        return call(wrapped, a)
    }

    fun call(f: suspend (Int) -> String, a: Int): String {
        val spec = FunctionSpec(intCoder, stringCoder)
        val id = getNextId()
        functionalServer.register(id, spec, f)
        val channel = transport.channel(contractorAddress, remoteFunctions.call)
        val argumentsBytes = PairCoder(stringCoder, intCoder).encode(Pair(id, a))
        val (answer, intermediateFlow) = placeOrder(argumentsBytes, channel)
        runBlocking {
            intermediateFlow.collect{
                println("Got ${it.type.stringRepr} as intermediate message")
            }
            answer.await()
        }
        return answer.getCompleted().decodeToString()
    }



    fun callUpTo(f: (Int) -> String, a: Int): Sequence<String> {
        TODO()
    }
}