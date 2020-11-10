## communicator-zmq

## Common

### Client

* ResultCallback - пара из обработчика успеха и обработчика ошибки
* Query - имя, адрес, аргумент, обработчик
* SpecCallback - пара из обработчика для найденой функции и ненайденной функции
* SpecQuery - имя, адрес, обработчик
* ClientState - набор из кучи разных штук:
    * zmq контекст
    * Дилер сокет
    * UUID
    * Очередь из Query и очередь из SpecQuery
    * Набор отправленных запросов(обычныъ/спек)
    * Набор сокетов
    * zmq reactor
* Client - хранит ClientState <br>
При создании запускает поток, который раз в NEW_QUERIES_QUEUE_UPDATE_INTERVAL проверяет, есть ли чт-то в очередях сообщений

### ClientForwardSocketHandler

* ForwardSocketHandlerArg - сокет + контекст клиента
* ClientState.handleForwardSocket - обработка ответа

### ZMQTransport

Реализация Transport