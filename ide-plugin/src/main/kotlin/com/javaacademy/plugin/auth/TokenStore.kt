package com.javaacademy.plugin.auth

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import java.util.Base64

object TokenStore {
    private const val SERVICE_NAME = "Java AI Academy"
    private const val REFRESH_TOKEN_KEY = "refresh_token"

    @Volatile
    private var accessToken: String? = null

    fun setTokens(newAccessToken: String, newRefreshToken: String) {
        accessToken = newAccessToken
        val attrs = credentialAttributes()
        PasswordSafe.instance.set(attrs, Credentials(REFRESH_TOKEN_KEY, newRefreshToken))
    }

    fun getAccessToken(): String? = accessToken

    fun getRefreshToken(): String? {
        return PasswordSafe.instance.getPassword(credentialAttributes())
    }

    fun clearTokens() {
        accessToken = null
        PasswordSafe.instance.set(credentialAttributes(), null)
    }

    fun isAuthenticated(): Boolean = accessToken != null

    fun extractEmail(jwt: String): String? {
        return try {
            val parts = jwt.split(".")
            if (parts.size < 2) return null
            val paddedPayload = padBase64(parts[1])
            val payload = String(Base64.getUrlDecoder().decode(paddedPayload))
            Regex("\"sub\"\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.get(1)
        } catch (_: Exception) {
            null
        }
    }

    private fun credentialAttributes(): CredentialAttributes =
        CredentialAttributes(generateServiceName(SERVICE_NAME, REFRESH_TOKEN_KEY))

    private fun padBase64(input: String): String = when (input.length % 4) {
        2 -> "$input=="
        3 -> "$input="
        else -> input
    }
}
