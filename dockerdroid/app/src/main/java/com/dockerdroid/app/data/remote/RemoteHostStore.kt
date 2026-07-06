package com.dockerdroid.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists any number of saved remote hosts and their SSH secrets using
 * [EncryptedSharedPreferences] (AES-256, key held in the Android Keystore), so
 * passwords / private keys are never stored in plaintext.
 *
 * Each host is keyed by [RemoteHost.id]; an index tracks the set of ids and which
 * one was used most recently (for auto-reconnect).
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

    private fun ids(): MutableSet<String> =
        prefs.getStringSet(KEY_INDEX, emptySet())!!.toMutableSet()

    fun save(host: RemoteHost, auth: SshAuth) {
        val id = host.id
        prefs.edit().apply {
            putStringSet(KEY_INDEX, ids().apply { add(id) })
            putString(KEY_LAST_USED, id)
            putString("$id.$KEY_LABEL", host.label)
            putString("$id.$KEY_HOST", host.host)
            putInt("$id.$KEY_PORT", host.port)
            putString("$id.$KEY_USER", host.username)
            putString("$id.$KEY_DIAL", host.dialCommand)
            when (auth) {
                is SshAuth.Password -> {
                    putString("$id.$KEY_AUTH_TYPE", AUTH_PASSWORD)
                    putString("$id.$KEY_SECRET", auth.password)
                    remove("$id.$KEY_PASSPHRASE")
                }
                is SshAuth.PrivateKey -> {
                    putString("$id.$KEY_AUTH_TYPE", AUTH_KEY)
                    putString("$id.$KEY_SECRET", auth.pem)
                    putString("$id.$KEY_PASSPHRASE", auth.passphrase)
                }
            }
            apply()
        }
    }

    fun listHosts(): List<RemoteHost> = ids().mapNotNull { loadHost(it) }

    fun loadHost(id: String): RemoteHost? {
        val host = prefs.getString("$id.$KEY_HOST", null) ?: return null
        return RemoteHost(
            label = prefs.getString("$id.$KEY_LABEL", host) ?: host,
            host = host,
            port = prefs.getInt("$id.$KEY_PORT", 22),
            username = prefs.getString("$id.$KEY_USER", "") ?: "",
            dialCommand = prefs.getString("$id.$KEY_DIAL", RemoteHost.DEFAULT_DIAL)
                ?: RemoteHost.DEFAULT_DIAL,
        )
    }

    fun loadAuth(id: String): SshAuth? {
        val secret = prefs.getString("$id.$KEY_SECRET", null) ?: return null
        return when (prefs.getString("$id.$KEY_AUTH_TYPE", AUTH_PASSWORD)) {
            AUTH_KEY -> SshAuth.PrivateKey(secret, prefs.getString("$id.$KEY_PASSPHRASE", null))
            else -> SshAuth.Password(secret)
        }
    }

    /** The most recently connected host + auth, for auto-reconnect. */
    fun loadLastUsed(): Pair<RemoteHost, SshAuth>? {
        val id = prefs.getString(KEY_LAST_USED, null) ?: ids().firstOrNull() ?: return null
        val host = loadHost(id) ?: return null
        val auth = loadAuth(id) ?: return null
        return host to auth
    }

    /** Back-compat convenience mirroring the old single-host API. */
    fun load(): Pair<RemoteHost, SshAuth>? = loadLastUsed()

    fun delete(id: String) {
        prefs.edit().apply {
            putStringSet(KEY_INDEX, ids().apply { remove(id) })
            listOf(KEY_LABEL, KEY_HOST, KEY_PORT, KEY_USER, KEY_DIAL, KEY_AUTH_TYPE, KEY_SECRET, KEY_PASSPHRASE)
                .forEach { remove("$id.$it") }
            if (prefs.getString(KEY_LAST_USED, null) == id) remove(KEY_LAST_USED)
            apply()
        }
    }

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_INDEX = "index"
        const val KEY_LAST_USED = "last_used"
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
