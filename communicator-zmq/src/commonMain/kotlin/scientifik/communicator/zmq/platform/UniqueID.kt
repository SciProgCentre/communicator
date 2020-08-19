package scientifik.communicator.zmq.platform

/** 16-byte unique query ID */
class UniqueID(val bytes: ByteArray) {
    /** Generates random query ID using UUID */
    constructor() : this(generateUuid())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UniqueID) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = uuidToString(bytes)
}

internal expect fun generateUuid(): ByteArray

/** Converts UUID to its canonical string representation (like "123e4567-e89b-12d3-a456-426655440000") */
internal expect fun uuidToString(bytes: ByteArray): String
