import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import space.kscience.communicator.api.IntCoder
import space.kscience.communicator.api.PairCoder
import transport.DummyRSocketTransport
import transport.RSocketClient

public suspend fun main() {
    val transport = DummyRSocketTransport()
    val ans = transport.respond("127.0.0.1:6789", "secret_function", PairCoder(IntCoder, IntCoder).encode(Pair(1, 2)))
    val result = IntCoder.decode(ans)
    println("EXAMPLE_JS_CLIENT: remote result is $result")
}