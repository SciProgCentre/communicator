package scientifik.communicator.zmq.platform

import java.nio.ByteBuffer

internal actual fun generateUuid(): ByteArray {
    val uuid = checkNotNull(java.util.UUID.randomUUID())
    val buffer: ByteBuffer = ByteBuffer.allocate(16)
    buffer.putLong(uuid.mostSignificantBits)
    buffer.putLong(uuid.leastSignificantBits)
    return buffer.array()
}


internal actual fun uuidToString(bytes: ByteArray): String {
    val buffer = checkNotNull(ByteBuffer.wrap(bytes))
    val mostSignificantBits: Long = buffer.long
    val leastSignificantBits: Long = buffer.long
    val uuid = java.util.UUID(mostSignificantBits, leastSignificantBits)
    return uuid.toString()
}