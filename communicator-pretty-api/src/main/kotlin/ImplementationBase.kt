package space.kscience.communicator.prettyapi

import space.kscience.communicator.api.*

internal abstract class ImplementationBase(
    val client: FunctionClient,
    val endpoint: ClientEndpoint,
    val declarations: Map<String, FunctionSet.Declaration<*, *>>,
) {
    @Suppress("INVISIBLE_MEMBER", "UNCHECKED_CAST")
    fun getRawFunction(name: String): suspend (Any) -> Any {
        val d = declarations[name]!!
        return client.transportCache
            .getOrPut(endpoint.protocol) {
                client.factory.client(endpoint.protocol)
                    ?: error("Protocol ${endpoint.protocol} is not supported by this client.")
            }
            .channel(endpoint.host, endpoint.port, d.name)
            .toFunction(d.argumentCodec as Codec<Any>, d.resultCodec as Codec<Any>)
    }
}