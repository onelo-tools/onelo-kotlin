package io.onelo

import kotlinx.serialization.json.Json

class OneloWaitlist internal constructor(private val client: _OneloClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun join(email: String, slug: String? = null): WaitlistResult {
        val body = buildMap<String, Any?> {
            put("email", email)
            if (slug != null) put("slug", slug)
        }
        val raw = client.post("/api/sdk/waitlist/join", body)
        return json.decodeFromString(WaitlistResult.serializer(), raw)
    }
}
