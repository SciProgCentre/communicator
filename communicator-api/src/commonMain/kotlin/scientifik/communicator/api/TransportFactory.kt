package scientifik.communicator.api

interface TransportFactory {
    operator fun get(protocol: String): Transport?
}