package com.javaacademy.plugin.auth

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

data class AuthResponse(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String
)

private data class LoginRequestBody(val email: String, val password: String)
private data class RefreshRequestBody(@Json(name = "refreshToken") val refreshToken: String)

class AuthClient(
    private val baseUrl: String,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val authResponseAdapter = moshi.adapter(AuthResponse::class.java)
    private val loginAdapter = moshi.adapter(LoginRequestBody::class.java)
    private val refreshAdapter = moshi.adapter(RefreshRequestBody::class.java)
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun login(email: String, password: String): AuthResponse {
        val body = loginAdapter.toJson(LoginRequestBody(email, password)).toRequestBody(jsonMedia)
        val request = Request.Builder()
            .url("$baseUrl/api/v1/auth/login")
            .post(body)
            .build()
        return execute(request)
    }

    fun refresh(refreshToken: String): AuthResponse {
        val body = refreshAdapter.toJson(RefreshRequestBody(refreshToken)).toRequestBody(jsonMedia)
        val request = Request.Builder()
            .url("$baseUrl/api/v1/auth/refresh")
            .post(body)
            .build()
        return execute(request)
    }

    private fun execute(request: Request): AuthResponse {
        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: throw IOException("Empty response body")
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $responseBody")
            }
            return authResponseAdapter.fromJson(responseBody)
                ?: throw IOException("Could not parse auth response")
        }
    }
}
