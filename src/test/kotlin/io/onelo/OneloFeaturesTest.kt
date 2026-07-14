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

class OneloFeaturesTest {
    private lateinit var server: MockWebServer
    private lateinit var features: OneloFeatures

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        features = OneloFeatures(_OneloClient("pk_test", server.url("/").toString().trimEnd('/')))
    }

    @AfterEach
    fun tearDown() { server.shutdown() }

    @Test
    fun `isEnabled returns true when status is ENABLED`() = runTest {
        server.enqueue(MockResponse().setBody("""{"features":{"export-button":{"status":"ENABLED"}},"ttl":300}"""))
        features.resolve()
        assertTrue(features.isEnabled("export-button"))
    }

    @Test
    fun `isEnabled returns false when status is DISABLED`() = runTest {
        server.enqueue(MockResponse().setBody("""{"features":{"export-button":{"status":"DISABLED"}},"ttl":300}"""))
        features.resolve()
        assertFalse(features.isEnabled("export-button"))
    }

    @Test
    fun `status returns UNKNOWN for missing feature`() = runTest {
        server.enqueue(MockResponse().setBody("""{"features":{},"ttl":300}"""))
        features.resolve()
        assertEquals(FeatureStatus.UNKNOWN, features.status("missing"))
    }

    @Test
    fun `resolve is not called again within TTL window`() = runTest {
        server.enqueue(MockResponse().setBody("""{"features":{"f":{"status":"ENABLED"}},"ttl":300}"""))
        features.resolve()
        features.resolve()
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `invalidateCache forces fresh resolve`() = runTest {
        server.enqueue(MockResponse().setBody("""{"features":{"f":{"status":"ENABLED"}},"ttl":300}"""))
        server.enqueue(MockResponse().setBody("""{"features":{"f":{"status":"DISABLED"}},"ttl":300}"""))
        features.resolve()
        features.invalidateCache()
        features.resolve()
        assertEquals(2, server.requestCount)
    }
}
