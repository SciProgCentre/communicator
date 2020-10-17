package kscience.communicator.zmq.proxy

import kscience.communicator.zmq.Protocol
import kscience.communicator.zmq.platform.UniqueID
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg

internal fun ZmqProxy.handleFrontend(frontend: ZmqSocket, backend: ZmqSocket) {
    val receivedMsg = ZmqMsg.recvMsg(frontend)
    val clientIdentity = receivedMsg.pop().data

    when (receivedMsg.pop().data.decodeToString()) {
        // Запрос на вычисление функции
        Protocol.Query -> {
            val queryID = receivedMsg.pop().data
            val queryArg = receivedMsg.pop().data
            val functionName = receivedMsg.pop().data.decodeToString()
            val worker = workersByFunction[functionName]?.randomOrNull()

            // Если воркера нет, возвращаем ошибку
            if (worker == null) sendMsg(frontend) {
                +clientIdentity
                +Protocol.Response.UnknownFunction
                +queryID
                +functionName
            }

            // Если воркер есть, передаем ему запрос
            else {
                sendMsg(backend) {
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
            val queryID = receivedMsg.pop().data
            val functionName = receivedMsg.pop().data.decodeToString()
            val schemesPair = functionSchemes[functionName]

            // Если функция зарегистрирована
            if (schemesPair != null) sendMsg(frontend) {
                +clientIdentity
                +Protocol.Coder.IdentityFound
                +queryID
                +schemesPair.first
                +schemesPair.second
            }

            // Если функция не зарегистрирована
            else sendMsg(frontend) {
                +clientIdentity
                +Protocol.Coder.IdentityNotFound
                +queryID
            }
        }

        // Сообщение о том, что ответ на запрос получен
        Protocol.Response.Received -> {
            val queryID = receivedMsg.pop().data
            sentResults.remove(UniqueID(queryID))
        }
    }
}
