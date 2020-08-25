package kscience.communicator.zmq.platform

import co.touchlab.stately.freeze
import kotlin.Long.Companion.SIZE_BITS
import kotlin.experimental.and
import kotlin.math.max
import kotlin.random.Random

@SharedImmutable
private val DIGITS: CharArray = charArrayOf(
    '0', '1', '2', '3', '4', '5',
    '6', '7', '8', '9', 'a', 'b',
    'c', 'd', 'e', 'f', 'g', 'h',
    'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't',
    'u', 'v', 'w', 'x', 'y', 'z'
).freeze()

private fun numberOfLeadingZeros(i: Long): Int {
    if (i == 0L) return 64
    var n = 1
    var x = (i ushr 32).toInt()

    if (x == 0) {
        n += 32
        x = i.toInt()
    }

    if (x ushr 16 == 0) {
        n += 16
        x = x shl 16
    }

    if (x ushr 24 == 0) {
        n += 8
        x = x shl 8
    }

    if (x ushr 28 == 0) {
        n += 4
        x = x shl 4
    }
    if (x ushr 30 == 0) {
        n += 2
        x = x shl 2
    }
    n -= x ushr 31
    return n
}

private class PseudoUuid(data: ByteArray) {
    internal var mostSigBits: Long = 0L
    internal var leastSigBits: Long = 0L

    init {
        var msb = 0L
        var lsb = 0L
        assert(data.size == 16) { "data must be 16 bytes in length" }
        var i = 0

        while (i < 8) {
            msb = msb shl 8 or (data[i] and 255.toByte()).toLong()
            ++i
        }

        i = 8

        while (i < 16) {
            lsb = lsb shl 8 or (data[i] and 255.toByte()).toLong()
            ++i
        }

        mostSigBits = msb
        leastSigBits = lsb
    }
}

private fun digits(`val`: Long, digits: Int): String? {
    val hi = 1L shl digits * 4
    val val1 = hi or (`val` and hi - 1)
    val chars = max((SIZE_BITS - numberOfLeadingZeros(val1) + (4 - 1)) / 4, 1)
    val buf = CharArray(chars)
    var val2 = val1
    var charPos = chars
    val mask = (1 shl 4) - 1

    do {
        buf[0 + --charPos] = DIGITS[val2.toInt() and mask]
        val2 = val2 ushr 4
    } while (val2 != 0L && charPos > 0)

    return String(buf).substring(1)
}

internal actual fun generateUuid(): ByteArray = Random.nextBytes(ByteArray(16))

/** Converts UUID to its canonical string representation (like "123e4567-e89b-12d3-a456-426655440000") */
internal actual fun uuidToString(bytes: ByteArray): String {
    val uuid = PseudoUuid(bytes)

    return digits(uuid.mostSigBits shr 32, 8) + "-" +
            digits(uuid.mostSigBits shr 16, 4) + "-" +
            digits(uuid.mostSigBits, 4) + "-" +
            digits(uuid.leastSigBits shr 48, 4) + "-" +
            digits(uuid.leastSigBits, 12)
}
