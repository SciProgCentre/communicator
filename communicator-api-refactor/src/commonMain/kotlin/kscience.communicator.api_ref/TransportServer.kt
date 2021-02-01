package kscience.communicator.api_ref

import kotlinx.io.Closeable

interface TransportServer: Closeable {

    fun register(name: String, function: PayloadFunction)

    fun unregister(name: String)
}