package com.par9uet.jm.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {
    companion object {
        private const val ENCRYPTED_PREFIX = "enc:"
        private const val PLAIN_PREFIX = "plain:"
        private const val GCM_IV_SIZE_BYTES = 12
    }

    private val keyAlias = "app_master_key"
    private val keyStore: KeyStore? = runCatching {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
    }.getOrNull()

    private fun getSecretKey(): SecretKey? {
        val store = keyStore ?: return null
        val existingKey = runCatching {
            store.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        }.getOrNull()
        if (existingKey != null) {
            return existingKey.secretKey
        }

        return runCatching {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val keySpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).apply {
                setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                setKeySize(256)
                setUserAuthenticationRequired(false)
            }.build()

            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        }.getOrNull()
    }

    fun encrypt(data: String): String {
        val encryptedData = runCatching {
            val key = getSecretKey() ?: return@runCatching null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val encrypted = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            ENCRYPTED_PREFIX + Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        }.getOrNull()
        if (encryptedData != null) {
            return encryptedData
        }

        return PLAIN_PREFIX + Base64.encodeToString(
            data.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
    }

    fun decrypt(encryptedData: String): String? {
        return when {
            encryptedData.startsWith(PLAIN_PREFIX) -> decodePlain(
                encryptedData.removePrefix(PLAIN_PREFIX)
            )

            encryptedData.startsWith(ENCRYPTED_PREFIX) -> decryptWithKeyStore(
                encryptedData.removePrefix(ENCRYPTED_PREFIX),
                ivAtStart = true
            )

            else -> decryptWithKeyStore(encryptedData, ivAtStart = false)
                ?: decodePlain(encryptedData)
        }
    }

    private fun decryptWithKeyStore(encryptedData: String, ivAtStart: Boolean): String? {
        return runCatching {
            val data = Base64.decode(encryptedData, Base64.NO_WRAP)
            if (data.size <= GCM_IV_SIZE_BYTES) {
                return@runCatching null
            }

            val iv = if (ivAtStart) {
                data.copyOfRange(0, GCM_IV_SIZE_BYTES)
            } else {
                data.copyOfRange(data.size - GCM_IV_SIZE_BYTES, data.size)
            }
            val encryptedBytes = if (ivAtStart) {
                data.copyOfRange(GCM_IV_SIZE_BYTES, data.size)
            } else {
                data.copyOfRange(0, data.size - GCM_IV_SIZE_BYTES)
            }
            val key = getSecretKey() ?: return@runCatching null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    private fun decodePlain(data: String): String? {
        return runCatching {
            String(Base64.decode(data, Base64.NO_WRAP), StandardCharsets.UTF_8)
        }.getOrNull()
    }
}
