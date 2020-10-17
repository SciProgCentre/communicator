package kscience.communicator.api

/**
 * Represents typed specification of function.
 *
 * @param T the type of argument.
 * @param R the type of result.
 * @property argumentCoder the coder of [T].
 * @property resultCoder the coder of [R].
 */
public data class FunctionSpec<T, R>(val argumentCoder: Coder<T>, val resultCoder: Coder<R>)

/**
 * Returned function will wrap serialization exceptions into [CoderException],
 * and will throw receiver function's exceptions as-is.
 */
public fun <T, R> (suspend (T) -> R).toBinary(spec: FunctionSpec<T, R>): PayloadFunction = { bin ->
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
public fun <T, R> PayloadFunction.toFunction(spec: FunctionSpec<T, R>): (suspend (T) -> R) = { arg ->
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
