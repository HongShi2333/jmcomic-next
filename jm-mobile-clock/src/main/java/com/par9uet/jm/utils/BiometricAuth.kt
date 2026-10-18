package com.par9uet.jm.utils

import android.content.Context
import android.content.ContextWrapper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

private const val BACKUP_BIOMETRIC_KEY_ALIAS = "jm_backup_biometric_binding"
private const val BACKUP_BIOMETRIC_PREFS = "jm_backup_biometric_prefs"
private const val BACKUP_INSTALLATION_ID = "installation_id"
private const val BACKUP_INSTALLATION_PREFIX = "install."
private const val BIOMETRIC_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

data class BiometricCapabilities(
    val canAuthenticate: Boolean,
    val hasFingerprintHardware: Boolean,
    val hasFaceHardware: Boolean,
)

fun biometricCapabilities(context: Context): BiometricCapabilities {
    val canAuthenticate = canUseBiometricAuth(context)
    return BiometricCapabilities(
        canAuthenticate = canAuthenticate,
        hasFingerprintHardware = context.packageManager.hasSystemFeature("android.hardware.fingerprint"),
        hasFaceHardware = context.packageManager.hasSystemFeature("android.hardware.biometrics.face"),
    )
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return current as? FragmentActivity
}

fun canUseBiometricAuth(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(BIOMETRIC_AUTHENTICATORS) ==
        BiometricManager.BIOMETRIC_SUCCESS

fun authenticateWithBiometrics(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {},
) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onError(errString.toString())
                }
            }
        },
    )
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(BIOMETRIC_AUTHENTICATORS)
        .setNegativeButtonText("使用密码或图形")
        .build()
    prompt.authenticate(promptInfo)
}

fun createBackupBiometricBinding(context: Context): String {
    val preferences = context.getSharedPreferences(BACKUP_BIOMETRIC_PREFS, Context.MODE_PRIVATE)
    val installationId = preferences.getString(BACKUP_INSTALLATION_ID, null)
        ?: UUID.randomUUID().toString().also {
            preferences.edit().putString(BACKUP_INSTALLATION_ID, it).apply()
        }
    return "$BACKUP_INSTALLATION_PREFIX$installationId"
}

fun verifyBackupBiometricBinding(context: Context, binding: String?): Boolean {
    if (binding.isNullOrBlank()) return false
    if (binding.startsWith(BACKUP_INSTALLATION_PREFIX)) {
        val localId = context.getSharedPreferences(BACKUP_BIOMETRIC_PREFS, Context.MODE_PRIVATE)
            .getString(BACKUP_INSTALLATION_ID, null) ?: return false
        return MessageDigest.isEqual(
            binding.removePrefix(BACKUP_INSTALLATION_PREFIX).toByteArray(Charsets.UTF_8),
            localId.toByteArray(Charsets.UTF_8),
        )
    }
    // 兼容此前已经成功使用 Android Keystore HMAC 创建的备份。
    val separator = binding.lastIndexOf('.')
    if (separator <= 0 || separator == binding.lastIndex) return false
    return runCatching {
        val payload = binding.substring(0, separator)
        val expected = signBackupBinding(payload)
        val actual = Base64.decode(
            binding.substring(separator + 1),
            Base64.NO_WRAP or Base64.URL_SAFE,
        )
        MessageDigest.isEqual(expected, actual)
    }.getOrDefault(false)
}

private fun signBackupBinding(payload: String): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(getOrCreateBackupBindingKey())
    return mac.doFinal(payload.toByteArray(Charsets.UTF_8))
}

private fun getOrCreateBackupBindingKey(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (keyStore.getKey(BACKUP_BIOMETRIC_KEY_ALIAS, null) as? SecretKey)?.let { return it }
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
    generator.init(
        KeyGenParameterSpec.Builder(
            BACKUP_BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build(),
    )
    return generator.generateKey()
}
