package kscience.communicator.zmq.platform

import platform.posix.errno
import kotlin.contracts.*

internal inline fun Int?.checkZeroMQCode(function: String = "", catch: ((CzmqException) -> Unit)) {
    contract { callsInPlace(catch, InvocationKind.AT_MOST_ONCE) }
    if (this != 0) catch(CzmqException(function, errno))
}

internal fun Int?.checkZeroMQCode(function: String = "") {
    if (this != 0) throw CzmqException(function, errno)
}

private fun findName(code: Int): ErrnoBase? = ErrnoBase.values().find { it.value == code }

internal class CzmqException : RuntimeException {
    internal constructor(
        function: String = "",
        code: Int
    ) : super("Code: $code - ${findName(code)} by function $function")

    internal constructor(function: String = "", code: Int, cause: Throwable?) : super(
        "Code: $code - ${findName(code)} by function $function",
        cause
    )
}
