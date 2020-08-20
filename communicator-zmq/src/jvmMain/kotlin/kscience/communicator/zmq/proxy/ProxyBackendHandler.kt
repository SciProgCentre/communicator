package kscience.communicator.zmq.proxy

import kscience.communicator.zmq.platform.UniqueID
import kscience.communicator.zmq.platform.ZmqSocket
import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer

internal fun ZmqProxy.handleBackend(frontend: ZmqSocket, backend: ZmqSocket) {
    val msg = backend.recvMsg()
    val workerIdentity = msg.pop().data

    when (msg.pop().data[0]) {
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
            workers.asSequence()
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
                } else if (existingSchemes.first != functionArgScheme || existingSchemes.second != functionResultScheme) {
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
            worker.functions.forEach { workersByFunction.computeIfAbsent(it) { mutableListOf() }.add(worker) }
        }
    }
}
