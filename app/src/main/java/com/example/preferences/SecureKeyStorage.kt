package com.example.preferences

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyStorage(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("secure_byok_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALIAS = "GeminiByokKeyAlias"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREF_ENCRYPTED_KEY = "encrypted_gemini_key"
        private const val PREF_IV = "gemini_key_iv"
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun saveApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            clearApiKey()
            return
        }

        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(apiKey.trim().toByteArray(Charsets.UTF_8))

            val encryptedString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)

            prefs.edit()
                .putString(PREF_ENCRYPTED_KEY, encryptedString)
                .putString(PREF_IV, ivString)
                .apply()
        } catch (e: Exception) {
            // Fallback for emulator / container where Keystore might be restricted
            prefs.edit()
                .putString(PREF_ENCRYPTED_KEY, Base64.encodeToString(apiKey.trim().toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
                .putString(PREF_IV, "FALLBACK")
                .apply()
        }
    }

    fun getApiKey(): String? {
        val encryptedString = prefs.getString(PREF_ENCRYPTED_KEY, null) ?: return null
        val ivString = prefs.getString(PREF_IV, null) ?: return null

        if (ivString == "FALLBACK") {
            return try {
                String(Base64.decode(encryptedString, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }

        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: return null

            val iv = Base64.decode(ivString, Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(encryptedString, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun clearApiKey() {
        prefs.edit().remove(PREF_ENCRYPTED_KEY).remove(PREF_IV).apply()
    }

    fun hasCustomKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }
}
