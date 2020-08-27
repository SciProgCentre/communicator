package kscience.communicator.zmq.client

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.io.Closeable
import kotlinx.io.use
import kscience.communicator.api.Payload
import kscience.communicator.zmq.platform.*
import mu.KLogger
import mu.KotlinLogging
import kscience.communicator.zmq.platform.UniqueID
import kscience.communicator.zmq.platform.ZmqContext
import kscience.communicator.zmq.platform.ZmqLoop
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg

private const val NEW_QUERIES_QUEUE_UPDATE_INTERVAL = 1

internal typealias ResultHandler = (ByteArray) -> Unit
internal typealias ErrorHandler = (Throwable) -> Unit

internal class ResultCallback(val onResult: ResultHandler, val onError: ErrorHandler)

internal class Query(
    val functionName: String,
    val address: String,
    val arg: Payload,
    val callback: ResultCallback
)

internal class SpecCallback(val onSpecFound: (String, String) -> Unit, val onSpecNotFound: () -> Unit)

internal class SpecQuery(
    val functionName: String,
    val address: String,
    val callback: SpecCallback
)

internal class ClientState(
    val ctx: ZmqContext = ZmqContext(),
    val mainDealer: ZmqSocket = ctx.createDealerSocket(),
    val identity: UniqueID = UniqueID(),
    // В эту очередь попадают запросы при вызове remoteFunction.invoke()
    val newQueriesQueue: Channel<Query> = Channel(BUFFERED),
    val specQueriesQueue: Channel<SpecQuery> = Channel(BUFFERED),
    // В этот словарь попадают запросы, которые уже отправлены на сервер и сервер ответил о том, что он получил их
    val queriesInWork: MutableMap<UniqueID, ResultCallback> = hashMapOf(),
    val specQueriesInWork: MutableMap<UniqueID, SpecCallback> = hashMapOf(),
    val forwardSockets: MutableMap<String, ZmqSocket> = hashMapOf(),
    val reactor: ZmqLoop = ZmqLoop(ctx)
) : Closeable {
    val log: KLogger = KotlinLogging.logger(this::class.simpleName.orEmpty())

    override fun close() {
        newQueriesQueue.close()
        reactor.close()
        mainDealer.close()
        ctx.close()
    }
}

/**
 * Принимает запросы о вызове удаленной функции из любых потоков и вызывает коллбек при получении результата
 */
internal class Client : Closeable {
    private val log: KLogger = KotlinLogging.logger(this::class.simpleName.orEmpty())
    private val state: ClientState = ClientState()

    init {
        runInBackground({}) {
            with(state) {
                reactor.addTimer(
                    NEW_QUERIES_QUEUE_UPDATE_INTERVAL,
                    0,

                    { _, arg ->
                        arg as ClientState
                        arg.handleQueue()
                        arg.handleSpecQueue()
                        0
                    },

                    this
                )

                reactor.addReader(mainDealer, { _, _ -> 0 }, Unit)
                reactor.start()
            }
        }
    }

    suspend fun makeQuery(query: Query) {
        log.info { "Adding query ${query.functionName} to the internal queue" }
        state.newQueriesQueue.send(query)
    }

    suspend fun makeSpecQuery(specQuery: SpecQuery) {
        state.specQueriesQueue.send(specQuery)
    }

    override fun close(): Unit = state.close()
}

private fun ClientState.handleQueue() {
    while (true) {
        val query = newQueriesQueue.poll() ?: break
        log.info { "Making query ${query.functionName}" }
        val id = UniqueID()
        queriesInWork[id] = query.callback
        sendMsg(getForwardSocket(query.address)) {
            +"QUERY"
            +id
            +query.arg
            +query.functionName.encodeToByteArray()
        }
    }
}

private fun ClientState.handleSpecQueue() {
    while (true) {
        val specQuery = specQueriesQueue.poll() ?: break
        log.info { "Making spec query ${specQuery.functionName}" }
        val id = UniqueID()
        specQueriesInWork[id] = specQuery.callback
        sendMsg(getForwardSocket(specQuery.address)) {
            +"CODER_IDENTITY_QUERY"
            +id
            +specQuery.functionName.encodeToByteArray()
        }
    }
}

private fun ClientState.getForwardSocket(address: String): ZmqSocket {
    val existing = forwardSockets[address]
    if (existing != null) return existing
    val forwardSocket = ctx.createDealerSocket()
    forwardSocket.setIdentity(identity.bytes)
    forwardSocket.connect("tcp://$address")

    reactor.addReader(
        forwardSocket,
        { _, _, arg ->
            arg as ForwardSocketHandlerArg
            arg.clientContext.handleForwardSocket(arg)
            0
        },
        ForwardSocketHandlerArg(forwardSocket, this)
    )

    forwardSockets[address] = forwardSocket
    return forwardSocket
}