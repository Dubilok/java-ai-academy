package com.javaacademy.plugin.auth

import org.junit.jupiter.api.Test
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenStoreTest {

    private fun buildJwt(email: String): String {
        val header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"sub":"$email","exp":9999999999}""".toByteArray())
        return "$header.$payload.fake-signature"
    }

    @Test
    fun `extractEmail_validJwt_returnsSubjectClaim`() {
        val jwt = buildJwt("student@example.com")
        assertEquals("student@example.com", TokenStore.extractEmail(jwt))
    }

    @Test
    fun `extractEmail_adminEmail_returnsSubjectClaim`() {
        val jwt = buildJwt("admin@javaacademy.com")
        assertEquals("admin@javaacademy.com", TokenStore.extractEmail(jwt))
    }

    @Test
    fun `extractEmail_malformedToken_returnsNull`() {
        assertNull(TokenStore.extractEmail("not.a.jwt.at.all.extra"))
    }

    @Test
    fun `extractEmail_onlyOneSegment_returnsNull`() {
        assertNull(TokenStore.extractEmail("onlyone"))
    }

    @Test
    fun `extractEmail_payloadWithoutSub_returnsNull`() {
        val header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"HS256"}""".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"exp":9999999999}""".toByteArray())
        val jwt = "$header.$payload.sig"
        assertNull(TokenStore.extractEmail(jwt))
    }
}
