package kscience.communicator.zmq_ref.zmq

import org.zeromq.ZLoop
import org.zeromq.ZMQ
import zmq.poll.PollItem

internal actual class ZmqLoop(private val actualLoop: ZLoop) {
    actual fun add(socket: ZmqSocket, handler: () -> Unit) {
        val actualHandler = ZLoop.IZLoopHandler { loop, item, arg ->
            handler()
            0
        }

        actualLoop.addPoller(
            ZMQ.PollItem(socket.actualSocket, ZMQ.Poller.POLLIN),
            actualHandler,
            null
        )
    }

    actual fun start() {
        actualLoop.start()
    }

    actual fun stop() {
        val stopHandler = ZLoop.IZLoopHandler { _, _, _ ->
            // the loop will stop if any handler returns -1
            -1
        }
        actualLoop.addTimer(1, 1, stopHandler, null)
    }
}
