package kscience.communicator.zmq.proxy

import kscience.communicator.zmq.Protocol
import kscience.communicator.zmq.platform.UniqueID
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg
import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer

internal fun ZmqProxy.handleBackend(frontend: ZmqSocket, backend: ZmqSocket) {
    val msg = ZmqMsg.recvMsg(frontend)
    val workerIdentity = msg.pop().data

    when (msg.pop().data.decodeToString()) {
        // Ответ на запрос - завершен успешно
        Protocol.Response.Result -> {
            val queryID = msg.pop().data
            val queryResult = msg.pop().data
            val clientIdentity = receivedQueries[UniqueID(queryID)]
            clientIdentity ?: return

            sendMsg(frontend) {
                +clientIdentity
                +Protocol.Response.Result
                +queryID
                +queryResult
            }

            sendMsg(backend) {
                +workerIdentity
                +Protocol.Response.Received
                +queryID
            }
        }

        // Ответ на запрос - ошибка RemoteFunctionException
        Protocol.Response.Exception -> {
            val queryID = msg.pop().data
            val remoteException = msg.pop().data
            val clientIdentity = receivedQueries[UniqueID(queryID)]
            clientIdentity ?: return

            sendMsg(frontend) {
                +clientIdentity
                +Protocol.Response.Exception
                +queryID
                +remoteException
            }

            sendMsg(backend) {
                +workerIdentity
                +Protocol.Response.Received
                +queryID
            }
        }

        // Сообщение о том, что запрос получен
        Protocol.QueryReceived -> {
            val queryID = msg.pop().data
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
            val functionsCount = ByteBuffer.wrap(msg.pop().data).int

            repeat(functionsCount) {
                val functionName = msg.pop().data.decodeToString()
                val functionArgScheme = msg.pop().data.decodeToString()
                val functionResultScheme = msg.pop().data.decodeToString()
                worker.functions += functionName
                val existingSchemes = functionSchemes[functionName]

                if (existingSchemes == null) {
                    functionSchemes[functionName] = Pair(
                        functionArgScheme,
                        functionResultScheme
                    )
                } else if (existingSchemes.first != functionArgScheme || existingSchemes.second != functionResultScheme) {
                    sendMsg(backend) {
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
