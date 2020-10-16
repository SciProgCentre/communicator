package kscience.communicator.api

/**
 * Base class for RemoteCall library exceptions.
 * These exceptions can be thrown by the suspend function returned by [FunctionClient.getFunction].
 * [IncompatibleSpecsException] can also be thrown by [FunctionServer.register] if it connects to a proxy as a worker,
 * and that proxy already has a worker with a different coder.
 */
sealed class RemoteCallException : Exception()

/**
 * This exception is thrown on the client if its coders have a different identities than the coders on the remote server.
 * This exception can also be thrown on the worker, if it tries to connect to a proxy,
 * and proxy already has a worker for the same function but with different coder identities.
 * Spec strings are the strings received from [FunctionSpec.toString] method, which uses [Coder.toString].
 */
class IncompatibleSpecsException(
    val functionName: String,
    val localSpec: String,
    val remoteSpec: String
) : RemoteCallException() {

    override val message: String?
        get() = """Remote server has different spec for the function.
                Function name: "$functionName"
                Local spec: $localSpec
                Remote spec: $remoteSpec
                """

}

class UnsupportedFunctionNameException(val functionName: String) : RemoteCallException() {

    override val message: String?
        get() = "Server does not support a function with name $functionName. " +
                "If you are using a proxy server, please make sure that required worker " +
                "is connected to the proxy before making query."
}

/**
 * This exception is thrown if the timeout for invoking remote function has ended,
 * and the client retried the query enough times.
 */
class TimeoutException : RemoteCallException() {
    override val message: String?
        get() = "Timeout for the query has ended."
}

/**
 * This exception is thrown if the remote function has thrown an exception which wasn't caught by its code.
 * This exception's stack trace is delivered to the client.
 * Worker logs its exception, but does not throw it from [FunctionServer] methods.
 */
class RemoteFunctionException(val remoteExceptionMessage: String) : RemoteCallException() {

    override val message: String?
        get() = "Remote function has finished with an exception: $remoteExceptionMessage"
}

/**
 * Base class for serialization/deserialization exception.
 * These exceptions are wrappers for exceptions thrown by [Coder.encode] or [Coder.decode].
 * If the [CoderException] happened on the remote server, it will be delivered to the client inside [RemoteFunctionException].
 */
sealed class CoderException : RemoteCallException()

/**
 * This exception is thrown if the coder can't serialize the object.
 */
class EncodingException(val obj: Any?, val coder: Coder<*>, val coderExceptionMessage: String) : CoderException() {
    override val message: String?
        get() = """Object serialization exception.
                Object: $obj
                Coder: $coder
                Coder identity: ${coder.identity}
                Exception message: $coderExceptionMessage
                """
}

/**
 * This exception is thrown if the coder can't deserialize the payload.
 */
class DecodingException(val payload: Payload, val coder: Coder<*>, val coderExceptionMessage: String) :
    CoderException() {

    override val message: String?
        get() = """Payload deserialization exception.
                Payload: ${payload.contentToString()}
                Coder: $coder
                Coder identity: ${coder.identity}
                Exception message: $coderExceptionMessage
                """
}
