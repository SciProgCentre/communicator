//import com.sun.security.ntlm.Client
//import kotlinx.coroutines.Deferred
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.async
//import scientifik.communicator.api.*
//import scientifik.communicator.userapi.LibraryClient
//
//class client : Client() {
//
//    object RemoteFunctions{
//        const val call = "call"
//    }
//
//    private val intCoder = IntCoder()
//    private val stringCoder = StringCoder()
//    private val stringListCoder = ListCoder(stringCoder)
//
//    fun call(f: (Int) -> String, a: Int): Deferred<String> {
//        return call(makeSuspend(f), a)
//    }
//
//    fun call(f: suspend (Int) -> String, a: Int): Deferred<String> {
//        val spec = FunctionSpec(intCoder, stringCoder)
//        val id = getNextId()
//        functionalServer.register(id, spec, f)
//        val channel = transport.channel(contractorAddress, RemoteFunctions.call)
//        val argumentsBytes = PairCoder(stringCoder, intCoder).encode(Pair(id, a))
//
//        return GlobalScope.async {
//            val responseBytes = channel(argumentsBytes)
//            stringCoder.decode(responseBytes)
//        }
//    }
//
//    fun callUpTo(f: suspend (Int) -> List<String>, a: Int): Deferred<List<String>> {
//        val spec = FunctionSpec(intCoder, stringListCoder)
//        val id = getNextId()
//        functionalServer.register(id, spec, f)
//        val channel = transport.channel(contractorAddress, RemoteFunctions.call)
//        val argumentsBytes = PairCoder(stringCoder, intCoder).encode(Pair(id, a))
//
//        return GlobalScope.async {
//            val responseBytes = channel(argumentsBytes)
//            stringListCoder.decode(responseBytes).toList()
//        }
//    }
//}
