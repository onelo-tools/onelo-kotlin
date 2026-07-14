package io.onelo

import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

private data class MonitorEvent(
    val featureName: String,
    val ok: Boolean,
    val durationMs: Long?,
    val error: String?,
    val meta: Map<String, Any>?,
    val userId: String?,
    val sessionId: String,
    val platform: String = "kotlin",
)

class OneloMonitor internal constructor(private val client: _OneloClient) {

    private val sessionId = UUID.randomUUID().toString()
    private val buffer = java.util.concurrent.ConcurrentLinkedDeque<MonitorEvent>()
    @Volatile private var currentUserId: String? = null
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val flushFuture: ScheduledFuture<*>

    init {
        flushFuture = scheduler.scheduleAtFixedRate({
            runBlocking { flush() }
        }, 15, 15, TimeUnit.SECONDS)
    }

    fun setUserId(userId: String?) { currentUserId = userId }

    fun event(featureName: String, ok: Boolean, durationMs: Long? = null, error: String? = null, meta: Map<String, Any>? = null) {
        if (buffer.size >= 200) return
        buffer.addLast(MonitorEvent(featureName, ok, durationMs, error, meta, currentUserId, sessionId))
    }

    suspend fun <T> track(featureName: String, block: suspend () -> T): T {
        val start = System.currentTimeMillis()
        return try {
            val result = block()
            event(featureName, ok = true, durationMs = System.currentTimeMillis() - start)
            result
        } catch (e: Exception) {
            event(featureName, ok = false, durationMs = System.currentTimeMillis() - start, error = e.message)
            throw e
        }
    }

    suspend fun flush() {
        if (buffer.isEmpty()) return
        val events = mutableListOf<MonitorEvent>()
        var e = buffer.pollFirst()
        while (e != null) { events.add(e); e = buffer.pollFirst() }
        try {
            val payload = buildMap<String, Any?> {
                put("events", events.map { e ->
                    buildMap {
                        put("featureName", e.featureName)
                        put("ok", e.ok)
                        put("platform", e.platform)
                        put("sessionId", e.sessionId)
                        if (e.durationMs != null) put("durationMs", e.durationMs)
                        if (e.error != null) put("error", e.error)
                        if (e.meta != null) put("meta", e.meta)
                        if (e.userId != null) put("userId", e.userId)
                    }
                })
            }
            client.post("/api/sdk/monitor/events/batch", payload)
        } catch (_: Exception) {
            // silent — monitoring must never crash the app
        }
    }

    fun destroy() {
        flushFuture.cancel(false)
        scheduler.shutdown()
    }
}
