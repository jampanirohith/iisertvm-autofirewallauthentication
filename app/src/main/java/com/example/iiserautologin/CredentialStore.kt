package com.example.iiserautologin

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the single IISER account locally using an AES-GCM key held by Android Keystore.
 * The email and password are encrypted together and are never written in plaintext
 * to the source code.
 */
object CredentialStore {

    private const val TAG = "CredentialStore"
    private const val PREFS_NAME = "secure_credentials"
    private const val PREF_CREDENTIALS = "encrypted_credentials"
    private const val KEY_ALIAS = "iiser_auto_login_credentials_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val SEPARATOR = "\u0000"

    data class Credentials(
        val email: String,
        val password: String
    )

    fun save(context: Context, email: String, password: String) {
        val key = getOrCreateKey()
        val plaintext = "$email$SEPARATOR$password".toByteArray(StandardCharsets.UTF_8)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val ciphertext = cipher.doFinal(plaintext)
        val combined = cipher.iv + ciphertext

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(
                PREF_CREDENTIALS,
                Base64.encodeToString(combined, Base64.NO_WRAP)
            )
            .apply()
    }

    fun get(context: Context): Credentials? {
        val encoded = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_CREDENTIALS, null)
            ?: return null

        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= 12) return null

            val iv = combined.copyOfRange(0, 12)
            val ciphertext = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            )

            val plaintext = String(
                cipher.doFinal(ciphertext),
                StandardCharsets.UTF_8
            )

            val separatorIndex = plaintext.indexOf(SEPARATOR)
            if (separatorIndex <= 0) return null

            val email = plaintext.substring(0, separatorIndex).trim()
            val password = plaintext.substring(separatorIndex + SEPARATOR.length)

            if (email.isBlank() || password.isEmpty()) return null

            Credentials(email, password)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to read saved credentials", e)
            null
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_CREDENTIALS)
            .apply()
    }

    fun hasCredentials(context: Context): Boolean {
        return get(context) != null
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )

        return keyGenerator.generateKey()
    }
}
