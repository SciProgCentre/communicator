package space.kscience.communicator.prettyapi

import space.kscience.communicator.api.ClientEndpoint
import space.kscience.communicator.api.Codec
import space.kscience.communicator.api.FunctionClient
import space.kscience.communicator.api.FunctionSet

internal abstract class ImplementationBase(
    val client: FunctionClient,
    val endpoint: ClientEndpoint,
    val declarations: Map<String, FunctionSet.Declaration<*, *>>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : Any, R : Any> getRawFunction(name: String): suspend (T) -> R {
        val d = declarations[name]!!
        return client.getFunction(endpoint, d.name, d.argumentCodec as Codec<T>, d.resultCodec as Codec<R>)
    }
}
