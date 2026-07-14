package io.onelo

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OneloFeedbackTest {

    private lateinit var server: MockWebServer
    private lateinit var client: _OneloClient
    private lateinit var features: OneloFeatures
    private val openedUrls = mutableListOf<String>()
    private lateinit var feedback: OneloFeedback

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = _OneloClient("pk_test", server.url("/").toString().trimEnd('/'))
        features = OneloFeatures(client)
        feedback = OneloFeedback(client, features, browserOpener = { openedUrls.add(it) })
        openedUrls.clear()
    }

    @AfterEach
    fun tearDown() { server.shutdown() }

    @Test
    fun `open sends GET to initiate and opens returned url`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"url":"https://app.onelo.tools/feedback/abc123"}"""))
        feedback.open()
        val req = server.takeRequest()
        assertTrue(req.path!!.startsWith("/api/sdk/feedback/initiate"))
        assertTrue(req.path!!.contains("key=pk_test"))
        assertEquals("https://app.onelo.tools/feedback/abc123", openedUrls.single())
    }

    @Test
    fun `open passes type and area as query params`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"url":"https://app.onelo.tools/feedback/x"}"""))
        feedback.open(FeedbackOptions(type = "bug", area = "auth"))
        val req = server.takeRequest()
        assertTrue(req.path!!.contains("type=bug"))
        assertTrue(req.path!!.contains("area=auth"))
    }

    @Test
    fun `open passes userId when provided`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"url":"https://app.onelo.tools/feedback/y"}"""))
        feedback.open(FeedbackOptions(userId = "user-42"))
        val req = server.takeRequest()
        assertTrue(req.path!!.contains("userId=user-42"))
    }

    @Test
    fun `open does nothing when server returns no url`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{}"""))
        feedback.open()
        assertTrue(openedUrls.isEmpty())
    }

    @Test
    fun `track is a no-op`() {
        feedback.track("checkout")
        // No exception = pass
    }
}
