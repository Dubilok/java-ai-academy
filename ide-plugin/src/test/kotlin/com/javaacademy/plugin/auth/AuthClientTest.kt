package com.javaacademy.plugin.auth

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: AuthClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = AuthClient(baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login_validCredentials_returnsTokens`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"accessToken":"access-abc","refreshToken":"refresh-xyz"}""")
        )

        val response = client.login("user@example.com", "secret")

        assertEquals("access-abc", response.accessToken)
        assertEquals("refresh-xyz", response.refreshToken)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/login", request.path)
    }

    @Test
    fun `login_wrongCredentials_throwsIoException`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"title":"Unauthorized"}""")
        )

        assertFailsWith<java.io.IOException> {
            client.login("user@example.com", "wrong")
        }
    }

    @Test
    fun `refresh_validToken_returnsNewTokens`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"accessToken":"new-access","refreshToken":"new-refresh"}""")
        )

        val response = client.refresh("old-refresh-token")

        assertEquals("new-access", response.accessToken)
        assertEquals("new-refresh", response.refreshToken)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/refresh", request.path)
    }

    @Test
    fun `refresh_expiredToken_throwsIoException`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"title":"Token expired"}""")
        )

        assertFailsWith<java.io.IOException> {
            client.refresh("expired-token")
        }
    }

    @Test
    fun `login_serverError_throwsIoException`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        assertFailsWith<java.io.IOException> {
            client.login("user@example.com", "secret")
        }
    }
}
