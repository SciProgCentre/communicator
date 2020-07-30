package scientifik.communicator.userapi

import scientifik.communicator.api.*

typealias LibraryFunction<A, R> = suspend (A) -> R

class LibraryFunctionBuilder<A, R>(
        val name: String,
        val spec: FunctionSpec<A, R>,
        val builder: suspend (Transport, String) -> LibraryFunction<A, R>)


abstract class LibraryContractor(
        protected val transportServer: TransportServer
) {
    open val functions = emptyList<LibraryFunctionBuilder<Any, Any>>()
    protected fun <A, R> toPayloadFunction(f: suspend (A) -> R, spec: FunctionSpec<A, R>): suspend (Payload) -> Payload {
        return {
            val arg = spec.argumentCoder.decode(it)
            spec.resultCoder.encode(f(arg))
        }
    }

    suspend fun <A, R>remoteCall(spec: FunctionSpec<A, R>, a: A, channel: PayloadFunction): R {
        return spec.resultCoder.decode(channel(spec.argumentCoder.encode(a)))
    }

    abstract suspend fun addFunctionalServer(transport: Transport, address: String)
    /*suspend fun addFunctionalServer(transport: Transport, address: String) {
        functions.forEach { f ->
            transportServer.register(f.name, toPayloadFunction(f.builder(transport, address), f.spec))
        }
    }*/
}