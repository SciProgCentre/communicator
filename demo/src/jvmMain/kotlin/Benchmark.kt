import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import space.kscience.communicator.api.*
import space.kscience.communicator.zmq.withZmq
import kotlin.system.measureTimeMillis

private val endpoint = ClientEndpoint("ZMQ", "127.0.0.1:8888")

fun simple_f(arg: Int): Int {
    return arg + arg;
}

fun string_f(str: String): Int {
    return str.length;
}

//fun higher_order(f: (String) -> Int, s: String): Int {
//    return f(s)
//}

private object BenchFunctions : FunctionSet(endpoint) {
    val f by declare(IntCoder, IntCoder)
    val s by declare(StringCoder, IntCoder)
}


fun main(): Unit = runBlocking {

    val server = TransportFunctionServer(BenchFunctions, TransportServerFactory.withZmq()) {
        it.impl(f) { x -> simple_f(x) }
        it.impl(s) { x -> string_f(x) }
    }

    val client = TransportFunctionClient(TransportClientFactory.withZmq())

    val num_samples = 2000
    val simple_args = (0 until num_samples).map {it}
    val string_args = (0 until num_samples).map { "a".repeat(it) }

    val f: (Int) -> Int =  { it ->
        runBlocking {BenchFunctions.f(client, it)}
    }

    val s: (String) -> Int = { it ->
//        println(it)
        runBlocking { BenchFunctions.s(client, it) }
    }

    println("running simple function...")
    val timeInMillisSimple = measureTimeMillis {
        simple_args.forEach { f(it) }
    }

    println("running string function...")
    val timeInMillisString = measureTimeMillis {
        string_args.forEach { s(it) }
    }

    println("Simple: $timeInMillisSimple milliseconds; ${timeInMillisSimple.toFloat() / simple_args.size.toFloat() * 1000.0f} microseconds per call")
    println("String: $timeInMillisString milliseconds; ${timeInMillisString.toFloat() / string_args.size.toFloat() * 1000.0f} microseconds per call")
    println()

    client.close()
    server.close()
}
