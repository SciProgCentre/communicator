package kscience.communicator.zmq.proxy

import kscience.communicator.zmq.Protocol
import kscience.communicator.zmq.platform.UniqueID
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg
import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer

internal fun ZmqProxy.handleBackend(frontend: ZmqSocket, backend: ZmqSocket) {
    val msg = ZmqMsg.recvMsg(frontend).use(::ArrayDeque)
    val workerIdentity = msg.removeFirst().data

    when (msg.removeFirst().data.decodeToString()) {
        // Ответ на запрос - завершен успешно
        Protocol.Response.Result -> {
            val queryID = msg.removeFirst().data
            val queryResult = msg.removeFirst().data
            val clientIdentity = receivedQueries[UniqueID(queryID)]
            clientIdentity ?: return

            frontend.sendMsg() {
                +clientIdentity
                +Protocol.Response.Result
                +queryID
                +queryResult
            }

            backend.sendMsg() {
                +workerIdentity
                +Protocol.Response.Received
                +queryID
            }
        }

        // Ответ на запрос - ошибка RemoteFunctionException
        Protocol.Response.Exception -> {
            val queryID = msg.removeFirst().data
            val remoteException = msg.removeFirst().data
            val clientIdentity = receivedQueries[UniqueID(queryID)]
            clientIdentity ?: return

            frontend.sendMsg {
                +clientIdentity
                +Protocol.Response.Exception
                +queryID
                +remoteException
            }

            backend.sendMsg {
                +workerIdentity
                +Protocol.Response.Received
                +queryID
            }
        }

        // Сообщение о том, что запрос получен
        Protocol.QueryReceived -> {
            val queryID = msg.removeFirst().data
            sentQueries.remove(UniqueID(queryID))
        }

        // Heart beat
        Protocol.HeartBeat -> {
            workers.asSequence()
                .filter { it.identity.contentEquals(workerIdentity) }
                .forEach { it.lastHeartbeatTime = currentTimeMillis() }
        }

        // Запрос воркера на подключение к прокси и передача всех схем
        Protocol.Worker.Register -> {
            val worker = Worker(identity = workerIdentity, lastHeartbeatTime = currentTimeMillis())
            val functionsCount = ByteBuffer.wrap(msg.removeFirst().data).int

            repeat(functionsCount) {
                val functionName = msg.removeFirst().data.decodeToString()
                val functionArgScheme = msg.removeFirst().data.decodeToString()
                val functionResultScheme = msg.removeFirst().data.decodeToString()
                worker.functions += functionName
                val existingSchemes = functionSchemes[functionName]

                if (existingSchemes == null) {
                    functionSchemes[functionName] = Pair(
                        functionArgScheme,
                        functionResultScheme
                    )
                } else if (existingSchemes.first != functionArgScheme || existingSchemes.second != functionResultScheme) {
                    backend.sendMsg {
                        +workerIdentity
                        +Protocol.IncompatibleSpecsFailure
                        +functionName
                        +existingSchemes.first
                        +existingSchemes.second
                    }

                    return
                }
            }

            workers += worker
            worker.functions.forEach { workersByFunction.getOrPut(it) { mutableListOf() } += worker }
        }
    }
}
