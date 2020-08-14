package scientifik.communicator.zmq.proxy

import org.zeromq.ZMQ
import org.zeromq.ZMsg
import scientifik.communicator.zmq.platform.UniqueID

internal fun ZmqProxy.handleFrontend(frontend: ZMQ.Socket, backend: ZMQ.Socket) {
    val receivedMsg = ZMsg.recvMsg(frontend)
    val clientIdentity = receivedMsg.pop().data
    val type = receivedMsg.pop().data[0]
    when (type) {

        // Запрос на вычисление функции
        11.toByte() -> {
            val queryID = receivedMsg.pop().data
            val queryArg = receivedMsg.pop().data
            val functionName = receivedMsg.pop().data.decodeToString()
            val worker = workersByFunction[functionName]?.randomOrNull()
            // Если воркера нет, возвращаем ошибку
            if (worker == null) sendMsg(frontend) {
                +clientIdentity
                +byteArrayOf(15)
                +queryID
                +functionName
            }
            // Если воркер есть, передаем ему запрос
            else {
                sendMsg(backend) {
                    +worker.identity
                    +byteArrayOf(11)
                    +queryID
                    +queryArg
                    +functionName
                }
                receivedQueries[UniqueID(queryID)] = clientIdentity
            }
        }

        // Запрос на получение структуры схемы для функции
        21.toByte() -> {
            val functionName = receivedMsg.pop().data.decodeToString()
            val schemesPair = functionSchemes[functionName]
            // Если функция зарегистрирована
            if (schemesPair != null) sendMsg(frontend) {
                +clientIdentity
                +byteArrayOf(21)
                +schemesPair.first
                +schemesPair.second
            }
            // Если функция не зарегистрирована
            else sendMsg(frontend) {
                +clientIdentity
                +byteArrayOf(22)
            }
        }

        // Сообщение о том, что ответ на запрос получен
        31.toByte() -> {
            val queryID = receivedMsg.pop().data
            sentResults.remove(UniqueID(queryID))
        }
    }
}