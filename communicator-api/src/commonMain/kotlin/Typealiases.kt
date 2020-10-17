package space.kscience.communicator.api

/**
 * Represents binary object used by communicator API to transfer arguments, results of function, etc.
 *
 * Currently, it is [ByteArray], but it can be replaced by more efficient platform specific blob types (JVM ByteBuffer,
 * JavaScript ArrayBuffer...); or it can be bound to kotlinx.io binary types.
 */
public typealias Payload = ByteArray

/**
 * Represents `suspend` function that takes and returns [Payload].
 */
public typealias PayloadFunction = suspend (Payload) -> Payload
