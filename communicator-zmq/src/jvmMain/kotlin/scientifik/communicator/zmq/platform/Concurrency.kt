package scientifik.communicator.zmq.platform

import kotlin.concurrent.thread

internal actual fun <T1, T2> runInBackground(
        supplier: () -> T1,
        volatileJob: (T1) -> T2
) {
    val arg = supplier()
    thread(isDaemon = true) {
        volatileJob(arg)
    }
}