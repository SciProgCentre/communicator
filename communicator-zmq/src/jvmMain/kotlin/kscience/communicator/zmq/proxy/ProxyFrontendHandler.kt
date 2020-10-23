package kscience.communicator.zmq.proxy

import kscience.communicator.zmq.Protocol
import kscience.communicator.zmq.platform.UniqueID
import kscience.communicator.zmq.platform.ZmqFrame
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg

internal fun ZmqProxy.handleFrontend(frontend: ZmqSocket, backend: ZmqSocket) {
    var receivedMsg = ZmqMsg.recvMsg(frontend).use { it.map(ZmqFrame::data) }
    val (clientIdentity, type) = receivedMsg
    receivedMsg = receivedMsg.drop(2)

    when (type.decodeToString()) {
        // Запрос на вычисление функции
        Protocol.Query -> {
            val (queryID, queryArg, functionName) = receivedMsg
            val worker = workersByFunction[functionName.decodeToString()]?.randomOrNull()

            // Если воркера нет, возвращаем ошибку
            if (worker == null) frontend.sendMsg {
                +clientIdentity
                +Protocol.Response.UnknownFunction
                +queryID
                +functionName.decodeToString()
            }

            // Если воркер есть, передаем ему запрос
            else {
                backend.sendMsg {
                    +worker.identity
                    +Protocol.Query
                    +queryID
                    +queryArg
                    +functionName
                }

                receivedQueries[UniqueID(queryID)] = clientIdentity
            }
        }

        // Запрос на получение структуры схемы для функции
        Protocol.Coder.IdentityQuery -> {
            val (queryID, functionName) = receivedMsg
            val schemesPair = functionSchemes[functionName.decodeToString()]

            // Если функция зарегистрирована
            if (schemesPair != null) frontend.sendMsg {
                +clientIdentity
                +Protocol.Coder.IdentityFound
                +queryID
                +schemesPair.first
                +schemesPair.second
            }

            // Если функция не зарегистрирована
            else frontend.sendMsg {
                +clientIdentity
                +Protocol.Coder.IdentityNotFound
                +queryID
            }
        }

        // Сообщение о том, что ответ на запрос получен
        Protocol.Response.Received -> {
            val (queryID) = receivedMsg
            sentResults.remove(UniqueID(queryID))
        }
    }
}
