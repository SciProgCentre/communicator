package space.kscience.communicator.rsocket


import space.kscience.communicator.api.FunctionSpec
import space.kscience.communicator.api.Payload
import space.kscience.communicator.zmq.server.ZmqTransportServer
import transport.RSocketClient


public class JsProxyTransportServer(
    private val actualServer: ZmqTransportServer,
    browserAddress: String,
    browserPort: Int
) {
    private val client = RSocketClient(browserAddress, browserPort)
    public fun register(name: String, spec: FunctionSpec<*, *>) {
        val lambda: suspend (Payload) -> Payload = {payload: Payload ->
            client.respond(payload)
        }
        actualServer.register(name, lambda, spec)
    }

    public fun unregister(name: String) {
        actualServer.unregister(name)
    }
}