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
