package scientifik.communicator.api

/**
 * Coder object exposed to the communicator API to bind objects to [Payload]. Also provides identity.
 *
 * @param T the type of decoded and encoded object.
 */
interface Coder<T> {
    /**
     * Identity of the coder is some data that is equal for the structurally equal coders
     * (coders that work with equal types of data).
     * Coders on different processes (machines) are compared by their identity.
     * Identity may be called "hash code", but this property has completely different purposes than [Any.hashCode].
     */
    val identity: ByteArray

    /**
     * Decodes payload to object.
     *
     * @param payload the payload.
     * @return the decoded object.
     */
    fun decode(payload: Payload): T

    /**
     * Encodes object to payload.
     *
     * @param value the object.
     * @return the payload.
     */
    fun encode(value: T): Payload

    /**
     * Force to reimplement [toString]
     * This method will be primarily used to log "wrong coder" errors
     */
    override fun toString(): String
}
