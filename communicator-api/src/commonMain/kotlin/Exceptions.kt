package space.kscience.communicator.api

/**
 * Base class for library's exceptions. These exceptions can be thrown by the `suspend` function returned by
 * [FunctionClient.getFunction]. [IncompatibleSpecsException] can also be thrown by [FunctionServer.register] if it
 * connects to a proxy as a worker, and that proxy already has a worker with a different codec.
 */
public sealed class RemoteCallException : Exception {
    protected constructor() : super()
    protected constructor(message: String?) : super(message)
    protected constructor (message: String?, cause: Throwable?) : super(message, cause)
    protected constructor(cause: Throwable?) : super(cause)
}

/**
 * This exception is thrown on the client if its codecs have a different identities than the codecs on the remote
 * server. This exception can also be thrown on the worker, if it tries to connect to a proxy, and proxy already has a
 * worker for the same function but with different codec identities.
 */
public class IncompatibleSpecsException(
    public val functionName: String,
    public val localSpec: String,
    public val remoteSpec: String
) : RemoteCallException() {
    override val message: String
        get() = """Remote server has different spec for the function.
                Function name: "$functionName"
                Local spec: $localSpec
                Remote spec: $remoteSpec
                """
}

public class UnsupportedFunctionNameException(public val functionName: String) : RemoteCallException() {
    override val message: String
        get() = "Server does not support a function with name $functionName. " +
                "If you are using a proxy server, please make sure that required worker " +
                "is connected to the proxy before making query."
}

/**
 * This exception is thrown if the timeout for invoking remote function has ended,
 * and the client retried the query enough times.
 */
public class TimeoutException : RemoteCallException() {
    override val message: String
        get() = "Timeout for the query has ended."
}

/**
 * This exception is thrown if the remote function has thrown an exception which wasn't caught by its code.
 * This exception's stack trace is delivered to the client.
 * Worker logs its exception, but does not throw it from [FunctionServer] methods.
 */
public class RemoteFunctionException(public val remoteExceptionMessage: String) : RemoteCallException() {
    override val message: String
        get() = "Remote function has finished with an exception: $remoteExceptionMessage"
}

/**
 * Base class for serialization/deserialization exception.
 * These exceptions are wrappers for exceptions thrown by [Codec.encode] or [Codec.decode].
 * If the [CodecException] happened on the remote server, it will be delivered to the client inside [RemoteFunctionException].
 */
public sealed class CodecException : RemoteCallException {
    protected constructor() : super()
    protected constructor(message: String?) : super(message)
    protected constructor (message: String?, cause: Throwable?) : super(message, cause)
    protected constructor(cause: Throwable?) : super(cause)
}

/**
 * This exception is thrown if the codec can't serialize the object.
 */
public class EncodingException(
    public val obj: Any?,
    public val codec: Codec<*>,
    cause: Throwable,
) : CodecException(cause) {
    override val message: String
        get() = """Object serialization exception.
                Object: $obj
                Codec: $codec
                Codec identity: ${codec.identity}
                Exception message: ${cause?.message}
                """
}

/**
 * This exception is thrown if the codec can't deserialize the payload.
 */
public class DecodingException(
    public val payload: Payload,
    public val codec: Codec<*>,
    cause: Throwable,
) : CodecException(cause) {
    override val message: String
        get() = """Payload deserialization exception.
                Payload: ${payload.contentToString()}
                Codec: $codec
                Codec identity: ${codec.identity}
                Exception message: ${cause?.message}
                """
}
