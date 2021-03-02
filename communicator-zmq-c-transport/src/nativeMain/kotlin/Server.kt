import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.invoke
import space.kscience.communicator.api.Payload
import space.kscience.communicator.zmq.server.ZmqTransportServer

public class Server(private val port: UShort) {
    private var handle: ZmqTransportServer? = null

    public fun start() {
        if (handle == null) handle = ZmqTransportServer(port.toInt())
    }

//    public fun register(
//        name: String,
//        function: CPointer<CFunction<(argument: ByteArray, invariant: COpaquePointer) -> ByteArray>>,
//        invariant: COpaquePointer,
//        argumentCoder: String,
//        resultCoder: String,
//    ) {
//        handle?.register(name, { function(it, invariant) }, argumentCoder to resultCoder)
//    }
}

public fun register(f: CPointer<CFunction<(Int) -> Int>>) {
}
