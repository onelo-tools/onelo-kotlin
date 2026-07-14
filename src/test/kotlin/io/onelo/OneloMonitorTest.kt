package io.onelo

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OneloMonitorTest {
    private lateinit var server: MockWebServer
    private lateinit var monitor: OneloMonitor

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        monitor = OneloMonitor(_OneloClient("pk_test", server.url("/").toString().trimEnd('/')))
    }

    @AfterEach
    fun tearDown() {
        monitor.destroy()
        server.shutdown()
    }

    @Test
    fun `flush sends buffered events`() = runTest {
        monitor.event("feature_x", ok = true, durationMs = 100)
        server.enqueue(MockResponse().setResponseCode(200))
        monitor.flush()
        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertTrue(body.contains("feature_x"))
        assertTrue(body.contains("kotlin"))
    }

    @Test
    fun `flush does nothing when buffer is empty`() = runTest {
        monitor.flush()
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `track returns function result and buffers event`() = runTest {
        val result = monitor.track("compute") { 42 }
        assertEquals(42, result)
        server.enqueue(MockResponse().setResponseCode(200))
        monitor.flush()
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("compute"))
    }

    @Test
    fun `track buffers failure event when block throws`() = runTest {
        try {
            monitor.track("risky") { throw RuntimeException("boom") }
        } catch (_: Exception) {}
        server.enqueue(MockResponse().setResponseCode(200))
        monitor.flush()
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("risky"))
        assertTrue(body.contains("boom"))
    }
}
