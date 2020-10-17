package kscience.communicator.api

/**
 * Coder object exposed to the communicator API to bind objects to [Payload]. Also provides identity.
 *
 * @param T the type of decoded and encoded object.
 */
public interface Coder<T> {
    /**
     * Identity of the coder is some data that is equal for the structurally equal coders
     * (coders that work with equal types of data).
     * Coders on different processes (machines) are compared by their identity.
     * Identity may be called "hash code", but this property has completely different purposes than [Any.hashCode].
     */
    public val identity: String

    /**
     * Decodes payload to object.
     *
     * @param payload the payload.
     * @return the decoded object.
     */
    public fun decode(payload: Payload): T

    /**
     * Encodes object to payload.
     *
     * @param value the object.
     * @return the payload.
     */
    public fun encode(value: T): Payload

    /**
     * Force to reimplement [toString]
     * This method will be primarily used to log "wrong coder" errors
     */
    override fun toString(): String
}
