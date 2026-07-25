package com.javaacademy.plugin.api

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ApiClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = ApiClient(baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchBootstrap_twoCoursesWithTasks_parsedCorrectly`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """[
                      {"id":"c1","title":"Java 21","technology":"Java","tasks":[
                        {"id":"t1","title":"Hello World","difficulty":"EASY","xpReward":10}
                      ]},
                      {"id":"c2","title":"Spring Boot","technology":"Spring","tasks":[]}
                    ]"""
                )
        )

        val fakeToken = buildFakeJwt()
        setFakeToken(fakeToken)

        val courses = client.fetchBootstrap()

        assertEquals(2, courses.size)
        assertEquals("Java 21", courses[0].title)
        assertEquals("Java", courses[0].technology)
        assertEquals(1, courses[0].tasks.size)
        assertEquals("Hello World", courses[0].tasks[0].title)
        assertEquals(10, courses[0].tasks[0].xpReward)
        assertTrue(courses[1].tasks.isEmpty())

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/ide/bootstrap", request.path)
        assertTrue(request.getHeader("Authorization")?.startsWith("Bearer ") == true)
        assertEquals("intellij-plugin/0.1.0", request.getHeader("X-Client"))
    }

    @Test
    fun `fetchBootstrap_emptyArray_returnsEmptyList`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]")
        )
        setFakeToken(buildFakeJwt())

        val courses = client.fetchBootstrap()

        assertTrue(courses.isEmpty())
    }

    @Test
    fun `fetchBootstrap_serverReturns401_throwsIoException`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"title":"Unauthorized"}"""))
        setFakeToken(buildFakeJwt())

        assertFailsWith<java.io.IOException> {
            client.fetchBootstrap()
        }
    }

    @Test
    fun `fetchTask_validId_returnsTaskDetail`() {
        val taskId = "task-uuid-123"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"id":"$taskId","title":"Hello World","templateCode":"public class Solution {}"}"""
                )
        )
        setFakeToken(buildFakeJwt())

        val task = client.fetchTask(taskId)

        assertEquals(taskId, task.id)
        assertEquals("Hello World", task.title)
        assertEquals("public class Solution {}", task.templateCode)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/tasks/$taskId", request.path)
    }

    @Test
    fun `fetchTask_noTemplate_returnsNullTemplateCode`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"id":"t1","title":"Task"}""")
        )
        setFakeToken(buildFakeJwt())

        val task = client.fetchTask("t1")

        assertEquals(null, task.templateCode)
    }

    @Test
    fun `submitSolution_validSource_returnsSubmissionId`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"submissionId":"sub-999"}""")
        )
        setFakeToken(buildFakeJwt())

        val submissionId = client.submitSolution("task-123", "public class Solution {}")

        assertEquals("sub-999", submissionId)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/tasks/task-123/submissions", request.path)
    }

    @Test
    fun `pollSubmission_passed_returnsPassedStatus`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":"PASSED","logs":null}""")
        )
        setFakeToken(buildFakeJwt())

        val status = client.pollSubmission("sub-999")

        assertEquals("PASSED", status.status)
        assertEquals(null, status.logs)
    }

    @Test
    fun `pollSubmission_failed_returnsFailedWithLogs`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":"FAILED","logs":"Test failed: expected 1 but was 0"}""")
        )
        setFakeToken(buildFakeJwt())

        val status = client.pollSubmission("sub-999")

        assertEquals("FAILED", status.status)
        assertEquals("Test failed: expected 1 but was 0", status.logs)
    }

    @Test
    fun `fetchHint_validTask_returnsSocraticHint`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"hint":"What does the equals() method check?"}""")
        )
        setFakeToken(buildFakeJwt())

        val hint = client.fetchHint("task-123")

        assertEquals("What does the equals() method check?", hint)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/tasks/task-123/ai-hint", request.path)
    }

    @Test
    fun `fetchHint_rateLimited_throwsIoException`() {
        server.enqueue(MockResponse().setResponseCode(429).setBody("""{"title":"Too Many Requests"}"""))
        setFakeToken(buildFakeJwt())

        assertFailsWith<java.io.IOException> {
            client.fetchHint("task-123")
        }
    }

    private fun buildFakeJwt(): String {
        val header = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"HS256"}""".toByteArray())
        val payload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"sub":"test@example.com"}""".toByteArray())
        return "$header.$payload.sig"
    }

    private fun setFakeToken(token: String) {
        val field = com.javaacademy.plugin.auth.TokenStore::class.java
            .getDeclaredField("accessToken")
        field.isAccessible = true
        field.set(com.javaacademy.plugin.auth.TokenStore, token)
    }
}
