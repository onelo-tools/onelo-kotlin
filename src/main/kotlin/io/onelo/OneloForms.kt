package io.onelo

import kotlinx.serialization.json.Json

class OneloForms internal constructor(private val client: _OneloClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun submit(
        formSlug: String,
        data: Map<String, String>,
        submitterEmail: String? = null
    ): FormResult {
        val body = buildMap<String, Any?> {
            put("formSlug", formSlug)
            put("data", data)
            submitterEmail?.let { put("submitterEmail", it) }
        }
        val raw = client.post("/api/sdk/forms/submit", body)
        return json.decodeFromString(FormResult.serializer(), raw)
    }
}
