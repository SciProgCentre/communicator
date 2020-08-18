package scientifik.communicator.userapi

import kotlinx.coroutines.runBlocking
import scientifik.communicator.api.Coder
import scientifik.communicator.api.IntCoder
import scientifik.communicator.api.log
import scientifik.communicator.api.logging

internal val TestCoder: Coder<Int> = IntCoder.logging()

internal object TestClient : Client("tcp://localhost:8888") {
    val f: F<Int> by function(coder = TestCoder)

    init {
        start()
    }
}

internal object TestServer : Server("tcp://localhost:8888") {
    val f: F<Int> by function(coder = TestCoder) { x -> x + 1 }

    init {
        start()
    }
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
