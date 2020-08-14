package scientifik.communicator.zmq.proxy

import org.zeromq.ZMQ
import org.zeromq.ZMsg
import scientifik.communicator.zmq.platform.UniqueID
import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer

internal fun ZmqProxy.handleBackend(frontend: ZMQ.Socket, backend: ZMQ.Socket) {
    val msg = ZMsg.recvMsg(backend)
    val workerIdentity = msg.pop().data
    val type = msg.pop().data[0]
    when (type) {

        // Ответ на запрос - завершен успешно
        11.toByte() -> {
            val queryID = msg.pop().data
            val queryResult = msg.pop().data
            val clientIdentity = receivedQueries[UniqueID(queryID)]
            clientIdentity ?: return
            sendMsg(frontend) {
                +clientIdentity
                +byteArrayOf(11)
                +queryID
                +queryResult
            }
        }

        // Ответ на запрос - ошибка RemoteFunctionException
        12.toByte() -> {
            val queryID = msg.pop().data
            val remoteException = msg.pop().data
            val clientIdentity = receivedQueries[UniqueID(queryID)]
            clientIdentity ?: return
            sendMsg(frontend) {
                +clientIdentity
                +byteArrayOf(12)
                +queryID
                +remoteException
            }
        }

        // Ответ на запрос - ошибка RemoteDecodingException
        13.toByte() -> {
            val queryID = msg.pop().data
            val remoteArgSchemeStructure = msg.pop().data
            val clientIdentity = receivedQueries[UniqueID(queryID)]
            clientIdentity ?: return
            sendMsg(frontend) {
                +clientIdentity
                +byteArrayOf(13)
                +queryID
                +remoteArgSchemeStructure
            }
        }

        // Ответ на запрос - ошибка RemoteEncodingException
        14.toByte() -> {
            val queryID = msg.pop().data
            val resultString = msg.pop().data
            val remoteResultSchemeStructure = msg.pop().data
            val clientIdentity = receivedQueries[UniqueID(queryID)]
            clientIdentity ?: return
            sendMsg(frontend) {
                +clientIdentity
                +byteArrayOf(14)
                +queryID
                +resultString
                +remoteResultSchemeStructure
            }
        }

        // Сообщение о том, что запрос получен
        31.toByte() -> {
            val queryID = msg.pop().data
            sentQueries.remove(UniqueID(queryID))
        }

        // Heart beat
        41.toByte() -> {
            workers
                    .filter { it.identity.contentEquals(workerIdentity) }
                    .forEach { it.lastHeartbeatTime = currentTimeMillis() }
        }

        // Запрос воркера на подключение к прокси и передача всех схем
        51.toByte() -> {
            val worker = Worker(workerIdentity, arrayListOf(), currentTimeMillis())
            val functionsCount = ByteBuffer.wrap(msg.pop().data).int
            repeat(functionsCount) {
                val functionName = msg.pop().data.decodeToString()
                val functionArgScheme = msg.pop().data.decodeToString()
                val functionResultScheme = msg.pop().data.decodeToString()
                worker.functions.add(functionName)
                val existingSchemes = functionSchemes[functionName]
                if (existingSchemes == null) {
                    functionSchemes[functionName] = Pair(
                            functionArgScheme,
                            functionResultScheme
                    )
                }
                else if (existingSchemes.first != functionArgScheme || existingSchemes.second != functionResultScheme) {
                    sendMsg(backend) {
                        +workerIdentity
                        +byteArrayOf(41)
                        +functionName
                        +existingSchemes.first
                        +existingSchemes.second
                    }
                    return
                }
            }
            workers.add(worker)
            worker.functions.forEach {
                workersByFunction.computeIfAbsent(it) { arrayListOf() }.add(worker)
            }
        }
    }
}