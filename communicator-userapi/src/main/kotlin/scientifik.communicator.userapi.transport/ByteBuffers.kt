package scientifik.communicator.userapi.transport

import java.nio.ByteBuffer
import java.util.*

internal fun ByteBuffer.getUuid(): UUID {
    val msb = long
    val lsb = long
    return UUID(msb, lsb)
}

internal fun ByteBuffer.putUuid(value: UUID) {
    putLong(value.mostSignificantBits)
    putLong(value.leastSignificantBits)
}
