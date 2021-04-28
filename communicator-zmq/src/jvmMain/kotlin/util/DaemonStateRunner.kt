package space.kscience.communicator.zmq.util

import co.touchlab.stately.isolate.StateRunner
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

internal actual class DaemonStateRunner : StateRunner {
    internal val stateExecutor = Executors.newSingleThreadExecutor {
        Executors.defaultThreadFactory().newThread(it).also { thr -> thr.isDaemon = true }
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    actual override fun <R> stateRun(block: () -> R): R {
        val result = stateExecutor.submit(
            Callable {
                try {
                    Result.success(block())
                } catch (e: Throwable) {
                    Result.failure(e)
                }
            }
        ).get()

        return result.getOrThrow()
    }

    actual override fun stop() = stateExecutor.shutdown()
}
