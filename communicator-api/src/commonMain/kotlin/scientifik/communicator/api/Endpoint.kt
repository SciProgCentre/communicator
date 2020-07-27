package scientifik.communicator.api

data class Endpoint(
        val protocol: String,
        /**
         * Address string in the format "host:port".
         * For example, "localhost:1234" or "127.0.0.1:4321"
         */
        val address: String
) {

    val host: String
        get() = address.split(":")[0]

    val port: Int
        get() = address.split(":")[1].toInt()

}