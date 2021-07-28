@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

import space.kscience.communicator.api.ClientEndpoint
import space.kscience.communicator.api.FunctionClient
import space.kscience.communicator.api.FunctionSet
import space.kscience.communicator.prettyapi.ImplementationBase

internal class Foo(
    client: FunctionClient,
    endpoint: ClientEndpoint,
    declarations: Map<String, FunctionSet.Declaration<*, *>>,
) : ImplementationBase(client, endpoint, declarations), API {
    override suspend fun boo(j: Structure): Int = getRawFunction("boo")(j) as Int
}
