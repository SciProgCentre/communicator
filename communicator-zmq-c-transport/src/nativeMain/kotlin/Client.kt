import kotlinx.coroutines.runBlocking
import space.kscience.communicator.api.Payload
import space.kscience.communicator.zmq.client.ZmqTransport

public class Client {
    private val handle: ZmqTransport = ZmqTransport()

    public fun respond(address: String, name: String, payload: Payload) {
        runBlocking {
            handle.respond(address, name, payload)
        }
    }
}
