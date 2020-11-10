## communicator-transport

### DefaultTransportFactory

кажется, бесполезная штука

### TransportFunctionClient

Реализация FunctionClient, берёт транспорт из TransportFactory

### TransportFunctionServer

принимаю ендпоинты, получаю ендпоинты, но специфичного вида... мне это не нравится

ок, это проверка, но всё ещё не оч понятно зачем

почему zmqTransportServer принимает только порт, но не ip?

не очень пока понятноб зачем вообще пилить поддержку многих одновременно работающих одинаковых серверов

