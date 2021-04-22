import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import space.kscience.communicator.api.*
import transport.DummyRSocketTransport
import transport.RSocketClient

public suspend fun main() {
    println("starting example js client...")
    val transport = DummyRSocketTransport()
    println("created transport")
    try {
        val ans = transport.respond("127.0.0.1:6789", "f", IntCoder.encode(2))
        println("got answer")
        val result = IntCoder.decode(ans)
        println("EXAMPLE_JS_CLIENT: remote result is $result")
    } catch (e: Exception) {
        println("OOPS: caught exception: ${e.message}")
    } finally {
        println("I'm done here")
    }
}