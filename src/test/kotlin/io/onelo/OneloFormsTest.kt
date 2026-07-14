package io.onelo

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OneloFormsTest {
    private lateinit var server: MockWebServer
    private lateinit var forms: OneloForms

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        forms = OneloForms(_OneloClient("pk_test", server.url("/").toString().trimEnd('/')))
    }

    @AfterEach
    fun tearDown() { server.shutdown() }

    @Test
    fun `submit returns success result`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"message":"Submitted"}"""))
        val result = forms.submit("feedback", mapOf("message" to "hello"))
        assertTrue(result.success)
        assertEquals("Submitted", result.message)
    }

    @Test
    fun `submit sends formSlug and data in body`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"message":null}"""))
        forms.submit("contact", mapOf("name" to "Ada"))
        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("contact"))
        assertTrue(body.contains("Ada"))
    }

    @Test
    fun `submit sends submitterEmail when provided`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"message":null}"""))
        forms.submit("feedback", mapOf("msg" to "hi"), submitterEmail = "a@b.com")
        val recorded = server.takeRequest()
        assertTrue(recorded.body.readUtf8().contains("a@b.com"))
    }
}
