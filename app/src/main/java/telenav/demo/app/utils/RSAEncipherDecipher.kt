package telenav.demo.app.utils

import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

import javax.crypto.Cipher

class RSAEncipherDecipher(base64PublicKey: String?, base64PrivateKey: String?) {
    private val publicKey: PublicKey?
    private val privateKey: PrivateKey?

    fun decrypt(encryptedText: String?): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return Base64.getUrlEncoder().encodeToString(cipher.doFinal(Base64.getDecoder().decode(encryptedText)))
    }

    fun encrypt(plainText: String?): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return Base64.getEncoder().encodeToString(cipher.doFinal(Base64.getUrlDecoder().decode(plainText)))
    }

    companion object {
        fun getPrivateKey(base64PrivateKey: String?): PrivateKey? {
            var privateKey: PrivateKey? = null
            val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64PrivateKey))
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)
            return privateKey
        }

        fun getPublicKey(base64PublicKey: String?): PublicKey? {
            var publicKey: PublicKey? = null
            val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey))
            val keyFactory = KeyFactory.getInstance("RSA")
            publicKey = keyFactory.generatePublic(keySpec)
            return publicKey
        }
    }

    init {
        publicKey = getPublicKey(base64PublicKey)
        privateKey = getPrivateKey(base64PrivateKey)
    }
}
