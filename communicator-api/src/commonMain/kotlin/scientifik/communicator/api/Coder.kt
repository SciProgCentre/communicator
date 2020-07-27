package scientifik.communicator.api

typealias Payload = ByteArray

interface Coder<T> {

    /**
     * This method may throw any exception if the value does not match the coder.
     * This exception will be delivered to the client as either [EncodingException] or [RemoteEncodingException]
     */
    fun encode(value: T): Payload

    /**
     * This method may throw any exception if the payload does not match the coder.
     * This exception will be delivered to the client as either [LocalDecodingException] or [DecodingException]
     */
    fun decode(bin: Payload): T

    /**
     * Identity of the coder is some data that is equal for the structurally equal coders
     * (coders that work with equal types of data).
     * Coders on different processes (machines) are compared by their identity.
     * Identity may be called "hash code", but this property has completely different purposes than [Any.hashCode].
     */
    val identity: ByteArray

    /**
     * Force to reimplement [toString]
     * This method will be primarily used to log "wrong coder" errors
     */
    override fun toString(): String
}