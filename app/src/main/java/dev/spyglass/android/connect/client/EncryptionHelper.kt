package dev.spyglass.android.connect.client

import android.util.Base64
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ECDH key exchange + AES-256-GCM encryption for the Android client.
 * Mirrors the desktop EncryptionManager. Uses java.security (no BouncyCastle needed on Android).
 */
class EncryptionHelper {

    companion object {
        private const val CURVE = "secp256r1"
        private const val AES_KEY_BITS = 256
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private val INFO = "spyglass-connect-v1".toByteArray()
    }

    private val keyPair: KeyPair
    private var sharedKey: SecretKeySpec? = null
    private val secureRandom = SecureRandom()

    init {
        val keyGen = KeyPairGenerator.getInstance("EC")
        keyGen.initialize(ECGenParameterSpec(CURVE), secureRandom)
        keyPair = keyGen.generateKeyPair()
    }

    /** Get our public key as Base64 for transmission. */
    fun getPublicKeyBase64(): String =
        Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)

    /**
     * Derive shared AES-256 key from the desktop's public key.
     */
    fun deriveSharedKey(peerPublicKeyBase64: String) {
        val peerKeyBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
        val keyFactory = KeyFactory.getInstance("EC")
        val peerPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(peerKeyBytes))

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(peerPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        timber.log.Timber.d("ECDH provider: ${keyAgreement.provider.name}")
        timber.log.Timber.d("Shared secret (${sharedSecret.size} bytes): ${sharedSecret.take(8).joinToString("") { "%02x".format(it) }}...")
        timber.log.Timber.d("Our pubkey hash: ${keyPair.public.encoded.take(8).joinToString("") { "%02x".format(it) }}...")
        timber.log.Timber.d("Peer pubkey hash: ${peerKeyBytes.take(8).joinToString("") { "%02x".format(it) }}...")

        // HKDF-SHA256
        val prk = hkdfExtract(ByteArray(32), sharedSecret)
        val okm = hkdfExpand(prk, INFO, AES_KEY_BITS / 8)
        timber.log.Timber.d("Derived AES key (first 8): ${okm.take(8).joinToString("") { "%02x".format(it) }}...")
        sharedKey = SecretKeySpec(okm, "AES")
    }

    val isReady: Boolean get() = sharedKey != null

    /** Encrypt plaintext to Base64(IV + ciphertext + tag). */
    fun encrypt(plaintext: String): String {
        val key = sharedKey ?: throw IllegalStateException("Shared key not derived")
        val iv = ByteArray(GCM_IV_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val result = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(ciphertext, 0, result, iv.size, ciphertext.size)

        return Base64.encodeToString(result, Base64.NO_WRAP)
    }

    /** Decrypt Base64(IV + ciphertext + tag) to plaintext. */
    fun decrypt(encryptedBase64: String): String {
        val key = sharedKey ?: throw IllegalStateException("Shared key not derived")
        val data = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        val iv = data.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = data.copyOfRange(GCM_IV_BYTES, data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        return mac.doFinal(ikm)
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))

        val result = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0
        var i: Byte = 1

        while (offset < length) {
            mac.update(t)
            mac.update(info)
            mac.update(i)
            t = mac.doFinal()
            val toCopy = minOf(t.size, length - offset)
            System.arraycopy(t, 0, result, offset, toCopy)
            offset += toCopy
            i++
        }

        return result
    }
}
