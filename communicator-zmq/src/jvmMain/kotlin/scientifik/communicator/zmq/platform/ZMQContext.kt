package scientifik.communicator.zmq.platform

import org.zeromq.SocketType
import org.zeromq.ZContext

/** Constructor must create a context with its init method */
actual class ZMQContext actual constructor() {

    val backupContext = ZContext()

    actual fun createRouterSocket(): ZMQSocket =
            ZMQSocket(backupContext.createSocket(SocketType.ROUTER))

    actual fun createDealerSocket(): ZMQSocket =
            ZMQSocket(backupContext.createSocket(SocketType.DEALER))


    actual fun close() {
        backupContext.close()
    }

}