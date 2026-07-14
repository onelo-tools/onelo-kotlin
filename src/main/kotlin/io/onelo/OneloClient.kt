package io.onelo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class _OneloClient(
    val publishableKey: String,
    private val baseUrl: String
) {
    private val http = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun get(path: String, params: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val url = buildString {
                append(baseUrl)
                append(path)
                if (params.isNotEmpty()) {
                    append("?")
                    append(params.entries.joinToString("&") { (k, v) ->
                        "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
                    })
                }
            }
            val request = Request.Builder().url(url).get().build()
            val response = http.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw OneloApiException("HTTP ${response.code}: $responseBody", response.code)
            }
            responseBody
        }

    suspend fun post(path: String, body: Map<String, Any?>): String =
        withContext(Dispatchers.IO) {
            val allFields = body + mapOf("publishableKey" to publishableKey)
            val payload = buildJsonString(allFields)
            val request = Request.Builder()
                .url("$baseUrl$path")
                .post(payload.toRequestBody(mediaType))
                .build()
            val response = http.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw OneloApiException("HTTP ${response.code}: $responseBody", response.code)
            }
            responseBody
        }

    private fun anyToJsonElement(v: Any?): JsonElement = when {
        v == null -> JsonNull
        v is String -> JsonPrimitive(v)
        v is Boolean -> JsonPrimitive(v)
        v is Int -> JsonPrimitive(v)
        v is Long -> JsonPrimitive(v)
        v is Number -> JsonPrimitive(v)
        v is List<*> -> JsonArray(v.map { anyToJsonElement(it) })
        v is Map<*, *> -> JsonObject(v.entries.associate { (mk, mv) -> mk.toString() to anyToJsonElement(mv) })
        else -> JsonPrimitive(v.toString())
    }

    private fun buildJsonString(map: Map<String, Any?>): String {
        val element = buildJsonObject {
            map.forEach { (k, v) ->
                if (v != null) put(k, anyToJsonElement(v))
            }
        }
        return Json.encodeToString(JsonObject.serializer(), element)
    }
}
