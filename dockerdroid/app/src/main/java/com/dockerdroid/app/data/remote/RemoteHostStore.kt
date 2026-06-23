package com.dockerdroid.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists the saved remote host and its SSH secret using
 * [EncryptedSharedPreferences] (AES-256, key held in the Android Keystore), so the
 * password / private key is never stored in plaintext on the device.
 */
class RemoteHostStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "remote_hosts",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun save(host: RemoteHost, auth: SshAuth) {
        prefs.edit().apply {
            putString(KEY_LABEL, host.label)
            putString(KEY_HOST, host.host)
            putInt(KEY_PORT, host.port)
            putString(KEY_USER, host.username)
            putString(KEY_DIAL, host.dialCommand)
            when (auth) {
                is SshAuth.Password -> {
                    putString(KEY_AUTH_TYPE, AUTH_PASSWORD)
                    putString(KEY_SECRET, auth.password)
                    remove(KEY_PASSPHRASE)
                }
                is SshAuth.PrivateKey -> {
                    putString(KEY_AUTH_TYPE, AUTH_KEY)
                    putString(KEY_SECRET, auth.pem)
                    putString(KEY_PASSPHRASE, auth.passphrase)
                }
            }
            apply()
        }
    }

    fun load(): Pair<RemoteHost, SshAuth>? {
        val host = prefs.getString(KEY_HOST, null) ?: return null
        val remote = RemoteHost(
            label = prefs.getString(KEY_LABEL, host) ?: host,
            host = host,
            port = prefs.getInt(KEY_PORT, 22),
            username = prefs.getString(KEY_USER, "") ?: "",
            dialCommand = prefs.getString(KEY_DIAL, "docker system dial-stdio")
                ?: "docker system dial-stdio",
        )
        val secret = prefs.getString(KEY_SECRET, null) ?: return null
        val auth = when (prefs.getString(KEY_AUTH_TYPE, AUTH_PASSWORD)) {
            AUTH_KEY -> SshAuth.PrivateKey(secret, prefs.getString(KEY_PASSPHRASE, null))
            else -> SshAuth.Password(secret)
        }
        return remote to auth
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_LABEL = "label"
        const val KEY_HOST = "host"
        const val KEY_PORT = "port"
        const val KEY_USER = "user"
        const val KEY_DIAL = "dial"
        const val KEY_AUTH_TYPE = "auth_type"
        const val KEY_SECRET = "secret"
        const val KEY_PASSPHRASE = "passphrase"
        const val AUTH_PASSWORD = "password"
        const val AUTH_KEY = "key"
    }
}
