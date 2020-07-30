import scientifik.communicator.api.IntCoder
import scientifik.communicator.api.Payload
import scientifik.communicator.api.StringCoder
import scientifik.communicator.api.TransportServer

class contractor(
        val transportServer: TransportServer
) {
    val lib = lib()
    val intCoder = IntCoder()
    val stringCoder = StringCoder()

    init {
        transportServer.register("call", ::wrappedCall)
        transportServer.register("callUpTo", ::wrappedCallUpTo)
    }

    suspend fun wrappedCall(arg: Payload): Payload {

    }

    suspend fun wrappedCallUpTo(arg: Payload): Payload {

    }
}