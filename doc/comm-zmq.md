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

### ZmqTransport

Реализация Transport

### Platform/

Тоненькие обёртки zmq для использования в common - коде

### server/

### ZmqTransportServer

Имплементация TransportServer

Хранит отдельно функции, отдельно их спеки, очередь ответов, очередь функций на регистрацию/удаление, сокет

при создании запускает отдельный поток, в котором сидит ZmqReactor, который раз в _время_ проверяет очередь с ответами и очередь с добавлением/удалением функций, обрабатывает их и принимает заказы на выполнение функций через _frontend_ сокет

### ZmqWorker

Копипаста _ZmqTransportServer_, которая нигде и никогда не используется?

### TransportServerFrontendHandler

handleFrontend - обрабатывает запросы на вычиление к ZmqTransportServer

### WorkerFrontendHandler

Видимо, копипаста TransportServerFrontendHandler

### util/MsgBuilder

вспомогательные функции по работе с сообщениями

## JvmMain

### Platform/

Ничего интересного

### Proxy/

### ZmqProxy

* Worker - представление штуки, которая на самом деле может обработать запросы. id, список имён функций, время последнего отклика
* ZmqProxy - содержит лист воркеров, два сокета: фронтенд принимает запросы и перепосылает их worker-ам, бэкенд принимает ответы воркеров и перепосылает их заказчикам 

### JsMain/

не сделано