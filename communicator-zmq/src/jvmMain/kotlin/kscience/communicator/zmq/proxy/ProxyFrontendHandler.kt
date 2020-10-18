package kscience.communicator.zmq.proxy

import kscience.communicator.zmq.platform.UniqueID
import kscience.communicator.zmq.platform.ZmqMsg
import kscience.communicator.zmq.platform.ZmqSocket
import kscience.communicator.zmq.util.sendMsg

internal fun ZmqProxy.handleFrontend(frontend: ZmqSocket, backend: ZmqSocket) {
    val receivedMsg = ZmqMsg.recvMsg(frontend)
    val clientIdentity = receivedMsg.pop().data

    when (receivedMsg.pop().data.decodeToString()) {

        // Запрос на вычисление функции
        "QUERY" -> {
            val queryID = receivedMsg.pop().data
            val queryArg = receivedMsg.pop().data
            val functionName = receivedMsg.pop().data.decodeToString()
            val worker = workersByFunction[functionName]?.randomOrNull()

            // Если воркера нет, возвращаем ошибку
            if (worker == null) sendMsg(frontend) {
                +clientIdentity
                +"RESPONSE_UNKNOWN_FUNCTION"
                +queryID
                +functionName
            }

            // Если воркер есть, передаем ему запрос
            else {
                sendMsg(backend) {
                    +worker.identity
                    +"QUERY"
                    +queryID
                    +queryArg
                    +functionName
                }

                receivedQueries[UniqueID(queryID)] = clientIdentity
            }
        }

        // Запрос на получение структуры схемы для функции
        "CODER_IDENTITY_QUERY" -> {
            val queryID = receivedMsg.pop().data
            val functionName = receivedMsg.pop().data.decodeToString()
            val schemesPair = functionSchemes[functionName]

            // Если функция зарегистрирована
            if (schemesPair != null) sendMsg(frontend) {
                +clientIdentity
                +"CODER_IDENTITY_FOUND"
                +queryID
                +schemesPair.first
                +schemesPair.second
            }

            // Если функция не зарегистрирована
            else sendMsg(frontend) {
                +clientIdentity
                +"CODER_IDENTITY_NOT_FOUND"
                +queryID
            }
        }

        // Сообщение о том, что ответ на запрос получен
        "RESPONSE_RECEIVED" -> {
            val queryID = receivedMsg.pop().data
            sentResults.remove(UniqueID(queryID))
        }
    }
}
