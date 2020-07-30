import scientifik.communicator.api.*
import scientifik.communicator.userapi.*

class contractor(transportServer: TransportServer) : LibraryContractor(transportServer) {
    val lib = lib()
    private val intCoder = IntCoder()
    private val stringCoder = StringCoder()
    private val callBuilder = LibraryFunctionBuilder("call", FunctionSpec(PairCoder(stringCoder, intCoder), stringCoder)){transport, address ->
        val suspended: suspend (Pair<String, Int>) -> String = { (fid, argument) ->
            remoteCall(FunctionSpec(intCoder, stringCoder), argument, transport.channel(address, fid))
        }
        suspended
    }

    override suspend fun addFunctionalServer(transport: Transport, address: String) {
        transportServer.register(callBuilder.name, toPayloadFunction(callBuilder.builder(transport, address), callBuilder.spec))
    }
}