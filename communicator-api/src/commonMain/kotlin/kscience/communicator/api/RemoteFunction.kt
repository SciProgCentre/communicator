package kscience.communicator.api

data class RemoteFunction<T, R>(
        val name: String,
        val endpoint: Endpoint,
        val spec: FunctionSpec<T, R>
)