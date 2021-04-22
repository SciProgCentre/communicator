package transport

public typealias RSocketPayload = io.rsocket.kotlin.payload.Payload

/**
 * Send a request and receive a reply
 */
public expect class RSocketClient(host: String, port: Int) {
    public suspend fun respond(payload: RSocketPayload): RSocketPayload
}