package scientifik.communicator.userapi

import kotlinx.coroutines.runBlocking
import scientifik.communicator.api.Coder
import scientifik.communicator.api.IntCoder
import scientifik.communicator.api.log

internal val TestCoder: Coder<Int> = IntCoder

internal object TestClient : Client("tcp://localhost:8888") {
    val f: F<Int> by function(coder = TestCoder)
}

internal object TestServer : Server("tcp://localhost:8888") {
    val f: F<Int> by function(name = "f", coder = TestCoder) { x -> x + 1 }
}

fun main(): Unit = runBlocking {
    log("Initializing server")
    TestServer
    log("Initializing client")
    TestClient
    log("Result is ${TestClient.f(1)}")
    TestServer.close()
    TestClient.close()
}
