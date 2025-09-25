package kajilab.togawa.staywatchbeaconandroid.useCase

class CommonUtils {
    fun hexStringToByteArray(data: String): ByteArray {
        val result = ByteArray(data.length / 2)
        for (i in data.indices step 2) {
            val byte = data.substring(i, i + 2).toInt(16).toByte()
            result[i / 2] = byte
        }
        return result
    }

    fun toLongLE(bytes: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((bytes[offset + i].toLong() and 0xffL) shl (8 * i))
        }
        return result
    }
}