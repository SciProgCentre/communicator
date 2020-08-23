//package kscience.communicator.zmq.platform
//
//import org.w3c.dom.Worker
//import org.w3c.dom.url.URL
//import org.w3c.files.Blob
//import org.w3c.files.BlobPropertyBag
//import org.w3c.workers.ServiceWorkerGlobalScope
//import kotlin.contracts.InvocationKind
//import kotlin.contracts.contract
//
//internal external val self: ServiceWorkerGlobalScope
//
//internal actual inline fun <T> runInBackground(supplier: () -> T, noinline volatileJob: (T) -> Any) {
//    contract {
//        callsInPlace(supplier, InvocationKind.EXACTLY_ONCE)
//        callsInPlace(volatileJob)
//    }
//
//    val arg = supplier()
//    var worker: Worker? = null
//
//    worker = Worker(
//        URL.createObjectURL(
//            Blob(
//                arrayOf(
//                    {
//                        self.onmessage = {
//                            volatileJob(arg)
//                            worker?.terminate()
//                        }
//                    }()
//                ),
//
//                object : BlobPropertyBag {
//                    init {
//                        type = "text/javascript"
//                    }
//                }
//            )
//        )
//    ).also { it.postMessage(null) }
//}
