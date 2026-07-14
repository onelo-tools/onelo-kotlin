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

class OneloTest {
    private lateinit var server: MockWebServer
    private lateinit var onelo: Onelo

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        onelo = Onelo(publishableKey = "pk_test", baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @AfterEach
    fun tearDown() { server.shutdown() }

    @Test
    fun `identify sets plan on features`() = runTest {
        onelo.identify("user-123", plan = "pro")
        assertEquals("pro", onelo.features.userPlan)
    }

    @Test
    fun `features isEnabled works after resolve`() = runTest {
        server.enqueue(MockResponse().setBody("""{"features":{"export-button":{"status":"ENABLED"}},"ttl":300}"""))
        onelo.identify("user-123")
        onelo.features.resolve()
        assertTrue(onelo.features.isEnabled("export-button"))
    }


    @Test
    fun `forms submit returns FormResult`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"message":"ok"}"""))
        val result = onelo.forms.submit("feedback", mapOf("msg" to "hello"))
        assertTrue(result.success)
    }

    @Test
    fun `waitlist join returns WaitlistResult`() = runTest {
        server.enqueue(MockResponse().setBody("""{"success":true,"alreadyJoined":false}"""))
        val result = onelo.waitlist.join(email = "x@y.com", slug = "beta")
        assertTrue(result.success)
        assertFalse(result.alreadyJoined)
    }

    @Test
    fun `identify invalidates features cache`() = runTest {
        server.enqueue(MockResponse().setBody("""{"features":{"f":{"status":"ENABLED"}},"ttl":300}"""))
        server.enqueue(MockResponse().setBody("""{"features":{"f":{"status":"DISABLED"}},"ttl":300}"""))
        onelo.features.resolve()
        onelo.identify("user-123", plan = "pro")
        onelo.features.resolve()
        assertEquals(2, server.requestCount)
    }
}
