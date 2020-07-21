package scientifik.communicator.api

typealias Payload = ByteArray

interface Coder<T> {
    fun encode(value: T): Payload
    fun decode(bin: Payload): T

    /**
     * Force to reimplement [toString]
     */
    override fun toString(): String

    /**
     * Force to reimplement [hashCode]
     */
    override fun hashCode(): Int
}