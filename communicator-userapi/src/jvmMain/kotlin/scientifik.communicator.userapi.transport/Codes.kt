package scientifik.communicator.userapi.transport

const val REQUEST_EVALUATE: Byte = 11
const val REQUEST_CODER_ID: Byte = 21
const val REQUEST_REVOCATION: Byte = 31
const val RESPONSE_SUCCESS: Byte = 11
const val RESPONSE_FUNCTION_EXCEPTION: Byte = 12
const val RESPONSE_DECODING_EXCEPTION: Byte = 13
const val RESPONSE_ENCODING_EXCEPTION: Byte = 14
const val RESPONSE_FUNCTION_SUPPORTED: Byte = 21
const val RESPONSE_FUNCTION_UNSUPPORTED: Byte = 22
const val RESPONSE_RECEIVED: Byte = 31
