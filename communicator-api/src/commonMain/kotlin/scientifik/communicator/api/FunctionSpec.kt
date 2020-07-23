package scientifik.communicator.api

data class FunctionSpec<T, R>(val argumentCoder: Coder<T>, val resultCoder: Coder<R>)

fun <T, R> (suspend (T) -> R).toBinary(spec: FunctionSpec<T, R>): BinaryFunction = { bin ->
    val arg: T = spec.argumentCoder.decode(bin)
    val res: R = invoke(arg)
    spec.resultCoder.encode(res)
}

fun <T, R> BinaryFunction.toFunction(spec: FunctionSpec<T, R>): (suspend (T) -> R) = { arg ->
    val bin: Payload = spec.argumentCoder.encode(arg)
    val res: Payload = invoke(bin)
    spec.resultCoder.decode(res)
}