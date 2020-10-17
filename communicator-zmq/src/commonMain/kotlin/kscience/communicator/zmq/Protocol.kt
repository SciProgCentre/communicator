package kscience.communicator.zmq

internal object Protocol {
    const val HeartBeat = "HEART_BEAT"
    const val IncompatibleSpecsFailure = "INCOMPATIBLE_SPECS_FAILURE"

    const val Query = "QUERY"
    const val QueryReceived = "QUERY_RECEIVED"

    object Coder {
        private const val Namespace = "CODER_"
        const val IdentityQuery = Namespace + "IDENTITY_QUERY"
        const val IdentityFound = Namespace + "IDENTITY_FOUND"
        const val IdentityNotFound = Namespace + "IDENTITY_NOT_FOUND"
    }

    object Worker {
        private const val Namespace = "WORKER_"
        const val Register = Namespace + "REGISTER"
    }

    object Response {
        private const val Namespace = "RESPONSE_"
        const val Received = Namespace + "RECEIVED"
        const val Result = Namespace + "RESULT"
        const val Exception = Namespace + "EXCEPTION"
        const val UnknownFunction = Namespace + "UNKNOWN_FUNCTION"
    }
}
