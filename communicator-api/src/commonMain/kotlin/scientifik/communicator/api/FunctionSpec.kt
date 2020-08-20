package scientifik.communicator.api

data class FunctionSpec<T, R>(val argumentCoder: Coder<T>, val resultCoder: Coder<R>)
data class FunctionSpecIn<T, R>(val argumentCoder: Encoder<T>, val resultCoder: Decoder<R>)
data class FunctionSpecOut<T, R>(val argumentCoder: Decoder<T>, val resultCoder: Encoder<R>)

/**
 * Returned function will wrap serialization exceptions into [CoderException],
 * and will throw receiver function's exceptions as-is.
 */
fun <T, R> (suspend (T) -> R).toBinary(spec: FunctionSpec<T, R>): PayloadFunction = { bin ->
    val arg = try {
        spec.argumentCoder.decode(bin)
    } catch (ex: Exception) {
        throw DecodingException(bin, spec.argumentCoder, ex.message.orEmpty())
    }

    val res = invoke(arg)

    try {
        spec.resultCoder.encode(res)
    } catch (ex: Exception) {
        throw EncodingException(res, spec.resultCoder, ex.message.orEmpty())
    }
}

/**
 * Returned function will wrap serialization exceptions into [CoderException],
 * and will throw receiver function's exceptions as-is.
 */
fun <T, R> PayloadFunction.toFunction(spec: FunctionSpec<T, R>): (suspend (T) -> R) = { arg ->
    val bin = try {
        spec.argumentCoder.encode(arg)
    } catch (ex: Exception) {
        throw EncodingException(arg, spec.argumentCoder, ex.message.orEmpty())
    }

    val res = invoke(bin)

    try {
        spec.resultCoder.decode(res)
    } catch (ex: Exception) {
        throw DecodingException(res, spec.resultCoder, ex.message.orEmpty())
    }
}
