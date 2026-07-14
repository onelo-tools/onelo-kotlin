package io.onelo

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.net.URI

data class FeedbackOptions(
    val type: String? = null,
    val area: String? = null,
    val userId: String? = null,
)

class OneloFeedback internal constructor(
    private val client: _OneloClient,
    private val features: OneloFeatures,
    private val browserOpener: (String) -> Unit = { url ->
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    },
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** No-op shim — kept for API consistency with other SDKs. */
    fun track(@Suppress("UNUSED_PARAMETER") area: String) {}

    suspend fun open(options: FeedbackOptions = FeedbackOptions()) {
        val params = buildMap<String, String> {
            put("key", client.publishableKey)
            options.type?.let { put("type", it) }
            options.area?.let { put("area", it) }
            options.userId?.let { put("userId", it) }
            val active = features.getActiveFeatureNames()
            if (active.isNotEmpty()) {
                put("session", Json.encodeToString(JsonArray(active.map { JsonPrimitive(it) })))
            }
        }
        val raw = client.get("/api/sdk/feedback/initiate", params)
        val obj = json.parseToJsonElement(raw).jsonObject
        val url = obj["url"]?.jsonPrimitive?.content ?: return
        browserOpener(url)
    }
}
