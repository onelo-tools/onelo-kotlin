package io.onelo

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class OneloFeatures internal constructor(private val client: _OneloClient) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cache: FeaturesResolveResponse? = null
    private var cacheExpiry: Long = 0L

    var userPlan: String? = null

    suspend fun resolve() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (cache != null && now < cacheExpiry) return
            val body = buildMap<String, Any?> {
                userPlan?.let { put("userPlan", it) }
            }
            val raw = client.post("/api/sdk/features/resolve", body)
            cache = json.decodeFromString(FeaturesResolveResponse.serializer(), raw)
            cacheExpiry = System.currentTimeMillis() + (cache!!.ttl * 1000)
        }
    }

    internal fun invalidateCache() {
        cache = null
        cacheExpiry = 0L
    }

    fun feature(name: String): OneloFeatureHandle = OneloFeatureHandle(status(name))

    fun isEnabled(name: String): Boolean = feature(name).isEnabled()

    fun status(name: String): FeatureStatus {
        val entry = cache?.features?.get(name) ?: return FeatureStatus.UNKNOWN
        return when (entry.status.uppercase()) {
            "ENABLED"      -> FeatureStatus.ENABLED
            "DISABLED"     -> FeatureStatus.DISABLED
            "GREYED"       -> FeatureStatus.GREYED
            "HIDDEN"       -> FeatureStatus.HIDDEN
            "UPSELL"       -> FeatureStatus.UPSELL
            "NEW"          -> FeatureStatus.NEW
            "BETA"         -> FeatureStatus.BETA
            "COMING_SOON"  -> FeatureStatus.COMING_SOON
            else           -> FeatureStatus.UNKNOWN
        }
    }

    fun getActiveFeatureNames(): List<String> {
        val features = cache?.features ?: return emptyList()
        return features.entries
            .filter { it.value.status.uppercase() in setOf("ENABLED", "NEW", "BETA") }
            .map { it.key }
    }

    suspend fun batchPing(names: List<String>) {
        if (names.isEmpty()) return
        client.post("/api/sdk/features/batch-ping", mapOf("features" to names))
    }
}
