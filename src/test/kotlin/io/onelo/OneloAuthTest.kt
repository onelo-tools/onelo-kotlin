package io.onelo

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OneloAuthTest {
    private lateinit var server: MockWebServer
    private lateinit var auth: OneloAuth

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        auth = OneloAuth(_OneloClient("pk_test", server.url("/").toString().trimEnd('/')))
    }

    @AfterEach
    fun tearDown() { server.shutdown() }

    @Test
    fun `signIn returns session on success`() = runTest {
        server.enqueue(MockResponse().setBody("""
            {"access_token":"tok","refresh_token":"ref","expires_at":9999999999,
             "user":{"id":"u1","email":"a@b.com","role":"member","tenant_id":"t1","app_metadata":{"user_role":"member","tenant_id":"t1"}}}
        """.trimIndent()))
        val session = auth.signIn("a@b.com", "pass")
        assertEquals("tok", session.accessToken)
        assertEquals("a@b.com", session.user.email)
    }

    @Test
    fun `getSession returns null before signIn`() = runTest {
        assertNull(auth.getSession())
    }

    @Test
    fun `signOut clears session`() = runTest {
        server.enqueue(MockResponse().setBody("""
            {"access_token":"tok","refresh_token":"ref","expires_at":9999999999,
             "user":{"id":"u1","email":"a@b.com","role":"member","tenant_id":"t1","app_metadata":{"user_role":"member","tenant_id":"t1"}}}
        """.trimIndent()))
        auth.signIn("a@b.com", "pass")
        server.enqueue(MockResponse().setResponseCode(200))
        auth.signOut()
        assertNull(auth.getSession())
    }

    @Test
    fun `sendMagicLink calls magic-link endpoint`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true}"""))
        auth.sendMagicLink("a@b.com")
        val recorded = server.takeRequest()
        assert(recorded.path!!.contains("magic-link"))
        assert(recorded.body.readUtf8().contains("a@b.com"))
    }

    @Test
    fun `sendPasswordReset calls reset-password endpoint`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true}"""))
        auth.sendPasswordReset("a@b.com")
        val recorded = server.takeRequest()
        assert(recorded.path!!.contains("reset-password"))
    }
}
