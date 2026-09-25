package com.par9uet.jm.clock.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores account credentials and the app-lock PIN, never image or page data. */
class WatchSecureStore(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val crypto = WatchCrypto()

    fun credentials(): WatchCredentials? {
        val username = read(KEY_USERNAME).orEmpty()
        val password = read(KEY_PASSWORD).orEmpty()
        return WatchCredentials(username, password).takeIf {
            it.username.isNotBlank() && it.password.isNotBlank()
        }
    }

    fun account(): WatchAccount? {
        val username = read(KEY_USERNAME).orEmpty()
        if (username.isBlank()) return null
        return WatchAccount(
            id = read(KEY_ACCOUNT_ID)?.toIntOrNull() ?: 0,
            username = username,
            avatarUrl = read(KEY_AVATAR).orEmpty(),
            level = read(KEY_LEVEL)?.toIntOrNull() ?: 0,
            levelName = read(KEY_LEVEL_NAME).orEmpty(),
            favoriteCount = read(KEY_FAVORITE_COUNT)?.toIntOrNull() ?: 0,
            favoriteLimit = read(KEY_FAVORITE_LIMIT)?.toIntOrNull() ?: 0,
            coins = read(KEY_COINS)?.toIntOrNull() ?: 0,
        )
    }

    fun saveSession(account: WatchAccount, password: String) {
        preferences.edit().apply {
            putSecure(KEY_ACCOUNT_ID, account.id.toString())
            putSecure(KEY_USERNAME, account.username)
            putSecure(KEY_PASSWORD, password)
            putSecure(KEY_AVATAR, account.avatarUrl)
            putSecure(KEY_LEVEL, account.level.toString())
            putSecure(KEY_LEVEL_NAME, account.levelName)
            putSecure(KEY_FAVORITE_COUNT, account.favoriteCount.toString())
            putSecure(KEY_FAVORITE_LIMIT, account.favoriteLimit.toString())
            putSecure(KEY_COINS, account.coins.toString())
            apply()
        }
    }

    fun clearSession() {
        preferences.edit().apply {
            SESSION_KEYS.forEach { key -> remove(key) }
            apply()
        }
    }

    fun hasLockPin(): Boolean = read(KEY_LOCK_PIN).orEmpty().isNotEmpty()

    fun matchesLockPin(pin: String): Boolean {
        val saved = read(KEY_LOCK_PIN) ?: return false
        return MessageDigest.isEqual(
            saved.toByteArray(StandardCharsets.UTF_8),
            pin.toByteArray(StandardCharsets.UTF_8),
        )
    }

    fun saveLockPin(pin: String) {
        require(pin.length in APP_LOCK_PIN_LENGTHS && pin.all(Char::isDigit))
        preferences.edit().putSecure(KEY_LOCK_PIN, pin).apply()
    }

    fun clearLockPin() {
        preferences.edit().remove(KEY_LOCK_PIN).apply()
    }

    private fun read(key: String): String? =
        preferences.getString(key, null)?.let(crypto::decrypt)

    private fun android.content.SharedPreferences.Editor.putSecure(
        key: String,
        value: String,
    ): android.content.SharedPreferences.Editor = putString(key, crypto.encrypt(value))

    private companion object {
        const val FILE_NAME = "jm_mobile_clock_secure"
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_AVATAR = "avatar"
        const val KEY_LEVEL = "level"
        const val KEY_LEVEL_NAME = "level_name"
        const val KEY_FAVORITE_COUNT = "favorite_count"
        const val KEY_FAVORITE_LIMIT = "favorite_limit"
        const val KEY_COINS = "coins"
        const val KEY_LOCK_PIN = "lock_pin"
        val APP_LOCK_PIN_LENGTHS = setOf(4, 6)
        val SESSION_KEYS = listOf(
            KEY_ACCOUNT_ID,
            KEY_USERNAME,
            KEY_PASSWORD,
            KEY_AVATAR,
            KEY_LEVEL,
            KEY_LEVEL_NAME,
            KEY_FAVORITE_COUNT,
            KEY_FAVORITE_LIMIT,
            KEY_COINS,
        )
    }
}

private class WatchCrypto {
    private val keyStore = runCatching {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) return@runCatching null
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }.getOrNull()

    fun encrypt(value: String): String {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) return encodePlain(value)
        val encrypted = runCatching {
            val key = secretKey() ?: return@runCatching null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val payload = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            ENCRYPTED_PREFIX + Base64.encodeToString(cipher.iv + payload, Base64.NO_WRAP)
        }.getOrNull()
        return encrypted ?: encodePlain(value)
    }

    fun decrypt(value: String): String? = when {
        value.startsWith(PLAIN_PREFIX) -> decodePlain(value.removePrefix(PLAIN_PREFIX))
        value.startsWith(ENCRYPTED_PREFIX) -> decryptGcm(value.removePrefix(ENCRYPTED_PREFIX))
        else -> null
    }

    private fun secretKey(): SecretKey? {
        val store = keyStore ?: return null
        (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        return runCatching {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generator.generateKey()
        }.getOrNull()
    }

    private fun decryptGcm(encoded: String): String? = runCatching {
        val data = Base64.decode(encoded, Base64.NO_WRAP)
        if (data.size <= GCM_IV_SIZE_BYTES) return@runCatching null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey() ?: return@runCatching null,
            GCMParameterSpec(128, data.copyOfRange(0, GCM_IV_SIZE_BYTES)),
        )
        String(cipher.doFinal(data.copyOfRange(GCM_IV_SIZE_BYTES, data.size)), StandardCharsets.UTF_8)
    }.getOrNull()

    private fun encodePlain(value: String): String =
        PLAIN_PREFIX + Base64.encodeToString(value.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)

    private fun decodePlain(value: String): String? = runCatching {
        String(Base64.decode(value, Base64.NO_WRAP), StandardCharsets.UTF_8)
    }.getOrNull()

    private companion object {
        const val KEY_ALIAS = "jm_clock_secure_store"
        const val PLAIN_PREFIX = "plain:"
        const val ENCRYPTED_PREFIX = "enc:"
        const val GCM_IV_SIZE_BYTES = 12
    }
}
