package kajilab.togawa.staywatchbeaconandroid.useCase

import java.nio.ByteBuffer
import java.nio.ByteOrder

class SipHash24(private val k0: Long, private val k1: Long) {

    private var v0 = 0x736f6d6570736575L
    private var v1 = 0x646f72616e646f6dL
    private var v2 = 0x6c7967656e657261L
    private var v3 = 0x7465646279746573L
    private var totalLength = 0

    init {
        v0 = v0 xor k0
        v1 = v1 xor k1
        v2 = v2 xor k0
        v3 = v3 xor k1
    }

    private fun rotateLeft(x: Long, b: Int): Long = (x shl b) or (x ushr (64 - b))

    private fun sipRound() {
        v0 += v1; v1 = rotateLeft(v1, 13); v1 = v1 xor v0; v0 = rotateLeft(v0, 32)
        v2 += v3; v3 = rotateLeft(v3, 16); v3 = v3 xor v2
        v0 += v3; v3 = rotateLeft(v3, 21); v3 = v3 xor v0
        v2 += v1; v1 = rotateLeft(v1, 17); v1 = v1 xor v2; v2 = rotateLeft(v2, 32)
    }

    fun digest(data: ByteArray): Long {
        val bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        totalLength = data.size

        while (bb.remaining() >= 8) {
            val m = bb.long
            v3 = v3 xor m
            repeat(2) { sipRound() }
            v0 = v0 xor m
        }

        var last = (totalLength and 0xff).toLong() shl 56
        var shift = 0
        while (bb.hasRemaining()) {
            last = last or ((bb.get().toLong() and 0xff) shl shift)
            shift += 8
        }

        v3 = v3 xor last
        repeat(2) { sipRound() }
        v0 = v0 xor last

        v2 = v2 xor 0xff
        repeat(4) { sipRound() }

        return v0 xor v1 xor v2 xor v3
    }
}