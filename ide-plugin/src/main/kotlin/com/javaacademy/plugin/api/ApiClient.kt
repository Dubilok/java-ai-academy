package com.javaacademy.plugin.api

import com.javaacademy.plugin.auth.TokenStore
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

data class TaskStub(
    val id: String,
    val title: String,
    val difficulty: String,
    @Json(name = "xpReward") val xpReward: Int
)

data class CourseStub(
    val id: String,
    val title: String,
    val technology: String,
    val tasks: List<TaskStub>
)

data class TaskDetail(
    val id: String,
    val title: String,
    @Json(name = "templateCode") val templateCode: String?
)

class ApiClient(
    private val baseUrl: String,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val courseListType = Types.newParameterizedType(List::class.java, CourseStub::class.java)
    private val courseListAdapter = moshi.adapter<List<CourseStub>>(courseListType)

    fun fetchTask(taskId: String): TaskDetail {
        val accessToken = TokenStore.getAccessToken()
            ?: throw IOException("Not authenticated")
        val request = Request.Builder()
            .url("$baseUrl/api/v1/tasks/$taskId")
            .header("Authorization", "Bearer $accessToken")
            .header("X-Client", "intellij-plugin/0.1.0")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty response")
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $body")
            }
            return moshi.adapter(TaskDetail::class.java).fromJson(body)
                ?: throw IOException("Could not parse task response")
        }
    }

    fun fetchBootstrap(): List<CourseStub> {
        val accessToken = TokenStore.getAccessToken()
            ?: throw IOException("Not authenticated")
        val request = Request.Builder()
            .url("$baseUrl/api/v1/ide/bootstrap")
            .header("Authorization", "Bearer $accessToken")
            .header("X-Client", "intellij-plugin/0.1.0")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty response")
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $body")
            }
            return courseListAdapter.fromJson(body) ?: emptyList()
        }
    }
}
