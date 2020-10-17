package kscience.communicator.zmq.platform

import platform.posix.errno

internal fun Int?.checkReturnState(function: String = "") {
    if (this == -1) throw CzmqException(function, errno)
}

private fun findName(code: Int): ErrnoValue? = ErrnoValue.values().find { it.code == code }

internal class CzmqException internal constructor(function: String = "", code: Int) :
    RuntimeException("errno: $code - ${findName(code)} by function $function")
