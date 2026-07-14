package io.onelo

import kotlinx.serialization.json.*

class OneloAuth internal constructor(private val client: _OneloClient) {

    @Volatile private var currentSession: OneloSession? = null

    suspend fun signIn(email: String, password: String): OneloSession {
        val resp = client.post("/api/sdk/auth/signin", mapOf("email" to email, "password" to password))
        val session = parseSession(resp)
        currentSession = session
        return session
    }

    suspend fun signUp(email: String, password: String): OneloSession {
        val resp = client.post("/api/sdk/auth/signup", mapOf("email" to email, "password" to password))
        val session = parseSession(resp)
        currentSession = session
        return session
    }

    suspend fun signOut() {
        try {
            currentSession?.let { client.post("/api/sdk/auth/signout", mapOf("refreshToken" to it.refreshToken)) }
        } catch (_: Exception) {}
        currentSession = null
    }

    fun getSession(): OneloSession? = currentSession

    suspend fun refreshSession(): OneloSession? {
        val token = currentSession?.refreshToken ?: return null
        return try {
            val resp = client.post("/api/sdk/auth/refresh", mapOf("refreshToken" to token))
            val session = parseSession(resp)
            currentSession = session
            session
        } catch (_: Exception) {
            null
        }
    }

    suspend fun sendMagicLink(email: String, redirectTo: String? = null) {
        val body = mutableMapOf<String, Any?>("email" to email)
        if (redirectTo != null) body["redirectTo"] = redirectTo
        client.post("/api/sdk/auth/magic-link", body)
    }

    suspend fun sendPasswordReset(email: String, redirectTo: String? = null) {
        val body = mutableMapOf<String, Any?>("email" to email)
        if (redirectTo != null) body["redirectTo"] = redirectTo
        client.post("/api/sdk/auth/reset-password/request", body)
    }

    private fun parseSession(raw: String): OneloSession {
        val j = Json.parseToJsonElement(raw).jsonObject
        val userObj = j["user"]!!.jsonObject
        val meta = userObj["app_metadata"]?.jsonObject
        val user = OneloUser(
            id = userObj["id"]!!.jsonPrimitive.content,
            email = userObj["email"]!!.jsonPrimitive.content,
            role = meta?.get("user_role")?.jsonPrimitive?.content ?: userObj["role"]?.jsonPrimitive?.content ?: "member",
            tenantId = meta?.get("tenant_id")?.jsonPrimitive?.content ?: userObj["tenant_id"]?.jsonPrimitive?.content ?: "",
        )
        return OneloSession(
            accessToken = j["access_token"]!!.jsonPrimitive.content,
            refreshToken = j["refresh_token"]!!.jsonPrimitive.content,
            expiresAt = j["expires_at"]!!.jsonPrimitive.long,
            user = user,
        )
    }
}
