package kscience.communicator.api

public data class RemoteFunction<T, R>(
    val name: String,
    val endpoint: Endpoint,
    val spec: FunctionSpec<T, R>
)
