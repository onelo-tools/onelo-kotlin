package io.onelo

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OneloClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: _OneloClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = _OneloClient(
            publishableKey = "pk_test",
            baseUrl = server.url("/").toString().trimEnd('/')
        )
    }

    @AfterEach
    fun tearDown() { server.shutdown() }

    @Test
    fun `post returns body on 200`() = runTest {
        server.enqueue(MockResponse().setBody("""{"ok":true}""").setResponseCode(200))
        val result = client.post("/ping", emptyMap())
        assertTrue(result.contains("ok"))
    }

    @Test
    fun `post throws OneloApiException on 4xx`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"not found"}"""))
        assertThrows<OneloApiException> { client.post("/ping", emptyMap()) }
    }

    @Test
    fun `post sends publishableKey in body`() = runTest {
        server.enqueue(MockResponse().setBody("{}").setResponseCode(200))
        client.post("/ping", emptyMap())
        val recorded = server.takeRequest()
        assertTrue(recorded.body.readUtf8().contains("pk_test"))
    }

    @Test
    fun `post sends Content-Type application json`() = runTest {
        server.enqueue(MockResponse().setBody("{}").setResponseCode(200))
        client.post("/ping", emptyMap())
        val recorded = server.takeRequest()
        assertEquals("application/json; charset=utf-8", recorded.getHeader("Content-Type"))
    }
}
