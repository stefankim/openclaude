package com.dockerdroid.app.core

import android.app.Activity
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Thin wrapper over the framework [BiometricPrompt] (API 28+, well below our minSdk).
 * Used to confirm the user's identity before decrypting stored SSH credentials.
 */
object BiometricGate {

    /** Show the prompt and suspend until the user succeeds, fails, or cancels. */
    suspend fun authenticate(activity: Activity, title: String): Boolean =
        suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setNegativeButton("Cancel", activity.mainExecutor) { _, _ -> }
                .build()
            val cancel = CancellationSignal().also { sig ->
                cont.invokeOnCancellation { sig.cancel() }
            }
            prompt.authenticate(
                cancel,
                activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        if (cont.isActive) cont.resume(true)
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        if (cont.isActive) cont.resume(false)
                    }
                    override fun onAuthenticationFailed() { /* let the user retry */ }
                },
            )
        }
}
