package kajilab.togawa.staywatchbeaconandroid.useCase

import java.security.SecureRandom

class CommonUtils {
    fun hexStringToByteArray(data: String): ByteArray {
        // 文字数が奇数なら先頭に"0"を追加
        val normalized = if (data.length % 2 != 0) "0$data" else data

        val result = ByteArray(normalized.length / 2)
        for (i in normalized.indices step 2) {
            val byte = normalized.substring(i, i + 2).toInt(16).toByte()
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
    fun generateRandomHex(length: Int): String {
        val random = SecureRandom()
        val bytes = ByteArray(length / 2) // 16進数2文字で1バイト
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}