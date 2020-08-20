package kscience.communicator.zmq.proxy

import kscience.communicator.zmq.platform.UniqueID
import kscience.communicator.zmq.platform.ZmqSocket
import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer

internal fun ZmqProxy.handleBackend(frontend: ZmqSocket, backend: ZmqSocket) {
    val msg = backend.recvMsg()
    val workerIdentity = msg.pop().data

    when (msg.pop().data.decodeToString()) {
        // Ответ на запрос - завершен успешно
        "RESPONSE_RESULT" -> {
            val queryID = msg.pop().data
            val queryResult = msg.pop().data
            val clientIdentity = receivedQueries[UniqueID(queryID)]
            clientIdentity ?: return

            sendMsg(frontend) {
                +clientIdentity
                +"RESPONSE_RESULT"
                +queryID
                +queryResult
            }
            sendMsg(backend) {
                +workerIdentity
                +"RESPONSE_RECEIVED"
                +queryID
            }
        }

        // Ответ на запрос - ошибка RemoteFunctionException
        "RESPONSE_EXCEPTION" -> {
            val queryID = msg.pop().data
            val remoteException = msg.pop().data
            val clientIdentity = receivedQueries[UniqueID(queryID)]
            clientIdentity ?: return
            sendMsg(frontend) {
                +clientIdentity
                +"RESPONSE_EXCEPTION"
                +queryID
                +remoteException
            }
            sendMsg(backend) {
                +workerIdentity
                +"RESPONSE_RECEIVED"
                +queryID
            }
        }

        // Сообщение о том, что запрос получен
        "QUERY_RECEIVED" -> {
            val queryID = msg.pop().data
            sentQueries.remove(UniqueID(queryID))
        }

        // Heart beat
        "HEART_BEAT" -> {
            workers.asSequence()
                .filter { it.identity.contentEquals(workerIdentity) }
                .forEach { it.lastHeartbeatTime = currentTimeMillis() }
        }

        // Запрос воркера на подключение к прокси и передача всех схем
        "WORKER_REGISTER" -> {
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
                        +"INCOMPATIBLE_SPECS_FAILURE"
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
