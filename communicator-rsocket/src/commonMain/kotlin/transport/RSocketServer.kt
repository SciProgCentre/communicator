package transport

import space.kscience.communicator.api.Transport


public expect class RSocketServer(port: Int, transport: Transport) {
    public fun start()
}
