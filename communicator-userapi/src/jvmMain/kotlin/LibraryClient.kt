package scientifik.communicator.userapi

import scientifik.communicator.api.*
import scientifik.communicator.factories.DefaultFunctionServer
import scientifik.communicator.factories.DefaultTransportFactory

abstract class LibraryClient(
        protected val functionalServer: FunctionServer = DefaultFunctionServer(
            listOf(Endpoint("ZMQ", "127.0.0.1:3333"))),
        protected val contractorAddress: String = "127.0.0.1:4444",
        transportFactory: TransportFactory = DefaultTransportFactory(),
        protocol: String = "ZMQ") {
    protected val transport: Transport = transportFactory[protocol]!!
    private var lambdaId = 0

    protected fun getNextId(): String {
        return lambdaId++.toString()
    }
    protected fun <A, R> makeSuspend(f: (A) -> R): suspend (A) -> R {
        return {f(it)}
    }

    companion object{
        val stopCommand = "___stop"
    }
}