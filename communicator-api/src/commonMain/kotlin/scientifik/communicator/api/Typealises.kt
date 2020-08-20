package scientifik.communicator.api

typealias Payload = ByteArray
typealias PayloadFunction = suspend (Payload) -> Payload
