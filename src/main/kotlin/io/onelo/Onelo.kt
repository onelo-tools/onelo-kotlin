package io.onelo

class Onelo(
    publishableKey: String,
    baseUrl: String = "https://api.onelo.tools"
) {
    private val client = _OneloClient(publishableKey, baseUrl)

    val auth     = OneloAuth(client)
    val features = OneloFeatures(client)
    val forms    = OneloForms(client)
    val waitlist = OneloWaitlist(client)
    val monitor  = OneloMonitor(client)
    val feedback = OneloFeedback(client, features)

    suspend fun identify(userId: String, plan: String? = null) {
        if (features.userPlan != plan) {
            features.userPlan = plan
            features.invalidateCache()
        }
    }
}
