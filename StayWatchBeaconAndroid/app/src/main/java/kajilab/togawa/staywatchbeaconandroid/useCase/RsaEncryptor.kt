package kajilab.togawa.staywatchbeaconandroid.useCase
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

class RsaEncryptor {

    // ---- ここにPrivBeaconKey暗号化用公開鍵をハードコード(これは漏れても復号化ができず暗号化しかできないため大丈夫．Androidはファイル関係ややこいし)．というか端末にインストールされる時点でリバースエンジニアリングでほぼ全てわかってしまうためファイルにしてもハードコーディングしても一緒 ----
    private val publicKeyPem = """
        -----BEGIN PUBLIC KEY-----
        MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzk/LdrGbr5q8WiZ7s/cq
        74lxllGMgxWZs8UL57tnPMfpbHJFfL4RB8FuqEp2/sXvjxwM2qV+Pj5ltx1w65Xu
        u03C3rvgrjTJgRkjiAqpcEnSnt7Lu9Y2y++BMEPmhHFUV4Mt/NFwjXPiz+a05P4I
        qfmt753AVvzed8yKEWHd8PEkZqMUf25q/+66B7mUFVHbGeSCkRwU/PtWzOhSvErC
        +nmj0936N3Mg0hpqZn2AA5ggatQBmW/quhfvY1hOVQk7vaRJtLBQ2ZHvE0wme6xU
        TQhlNT3kwmUhKZbzRBglQLgPgGzsKx6OOKYSPZJx5srfzSRczUxSXibxsLqhGsgI
        8wIDAQAB
        -----END PUBLIC KEY-----
    """.trimIndent()
    // ---------------------------------

    private fun loadPublicKey(): PublicKey {
        // PEM ヘッダを削除して Base64 部分だけにする
        val base64Key = publicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        val decoded = Base64.getDecoder().decode(base64Key)
        val keySpec = X509EncodedKeySpec(decoded)

        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    /**
     * RSA-OAEP(SHA-256) により公開鍵で暗号化する
     */
    fun encrypt(plainText: String): String {
        val publicKey = loadPublicKey()

        // Go の rsa.DecryptOAEP(sha256.New()) と互換にする
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    fun generatePrivBeaconKey(): String {
        val bytes = ByteArray(16) // 16バイト = 128bit = 32 hex chars
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}