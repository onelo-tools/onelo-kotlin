package io.onelo

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OneloWaitlistTest {
    private lateinit var server: MockWebServer
    private lateinit var waitlist: OneloWaitlist

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        waitlist = OneloWaitlist(_OneloClient("pk_test", server.url("/").toString().trimEnd('/')))
    }

    @AfterEach
    fun tearDown() { server.shutdown() }

    @Test
    fun `join returns position on first join`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"position":42,"alreadyJoined":false}"""))
        val result = waitlist.join(email = "user@example.com", slug = "beta")
        assertTrue(result.success)
        assertEquals(42, result.position)
        assertFalse(result.alreadyJoined)
    }

    @Test
    fun `join sets alreadyJoined true on repeat`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"alreadyJoined":true}"""))
        val result = waitlist.join(email = "user@example.com", slug = "beta")
        assertTrue(result.alreadyJoined)
    }

    @Test
    fun `join sends slug and email in body`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"alreadyJoined":false}"""))
        waitlist.join(email = "user@example.com", slug = "beta")
        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"slug\":\"beta\""))
        assertTrue(body.contains("user@example.com"))
    }
}
