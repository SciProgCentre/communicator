package space.kscience.communicator.api

/**
 * Codec object exposed to the communicator API to bind objects to [Payload]. Also provides identity.
 *
 * @param T the type of decoded and encoded object.
 */
public interface Codec<T : Any> {
    /**
     * Identity of the codec is some data that is equal for the structurally equal codecs (codecs that work with equal
     * types of data).
     * Codecs on different processes (machines) are compared by their identity.
     * Identity may be called "hash code", but this property has completely different purposes than [Any.hashCode].
     */
    public val identity: String

    /**
     * Decodes payload to object.
     *
     * @param payload the payload.
     * @return the decoded object plus the quantity of bytes read from [payload].
     */
    public suspend fun decode(payload: Payload): Pair<T, Int>

    /**
     * Encodes object to payload.
     *
     * @param value the object.
     * @return the payload.
     */
    public suspend fun encode(value: T): Payload
}

/**
 * Returned function will wrap serialization exceptions into [CodecException],
 * and will throw receiver function's exceptions as-is.
 */
public fun <T : Any, R : Any> (suspend (T) -> R).toBinary(
    argumentCodec: Codec<T>,
    resultCodec: Codec<R>,
): PayloadFunction = { bin ->
    val (arg, _) = try {
        argumentCodec.decode(bin)
    } catch (ex: Exception) {
        throw DecodingException(bin, argumentCodec, ex)
    }

    val res = invoke(arg)

    try {
        resultCodec.encode(res)
    } catch (ex: Exception) {
        throw EncodingException(res, resultCodec, ex)
    }
}

/**
 * Returned function will wrap serialization exceptions into [CodecException],
 * and will throw receiver function's exceptions as-is.
 */
public fun <T : Any, R : Any> PayloadFunction.toFunction(
    argumentCodec: Codec<T>,
    resultCodec: Codec<R>,
): (suspend (T) -> R) = { arg ->
    val bin = try {
        argumentCodec.encode(arg)
    } catch (ex: Exception) {
        throw EncodingException(arg, argumentCodec, ex)
    }

    val res = invoke(bin)

    try {
        resultCodec.decode(res).first
    } catch (ex: Exception) {
        throw DecodingException(res, resultCodec, ex)
    }
}
