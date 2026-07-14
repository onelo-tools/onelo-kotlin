package io.onelo

import kotlinx.serialization.Serializable

// Feature types
enum class FeatureStatus { ENABLED, DISABLED, GREYED, HIDDEN, UPSELL, NEW, BETA, COMING_SOON, UNKNOWN }

class OneloFeatureHandle(val status: FeatureStatus) {
    fun isEnabled(): Boolean = status == FeatureStatus.ENABLED || status == FeatureStatus.NEW || status == FeatureStatus.BETA
    fun isVisible(): Boolean = status != FeatureStatus.HIDDEN
    fun isGreyed(): Boolean = status == FeatureStatus.GREYED
    fun isUpsell(): Boolean = status == FeatureStatus.UPSELL
    fun isDisabled(): Boolean = status == FeatureStatus.DISABLED
    fun isNew(): Boolean = status == FeatureStatus.NEW
    fun isBeta(): Boolean = status == FeatureStatus.BETA
    fun isComingSoon(): Boolean = status == FeatureStatus.COMING_SOON
    val badgeLabel: String? get() = when (status) {
        FeatureStatus.NEW          -> "New"
        FeatureStatus.BETA         -> "Beta"
        FeatureStatus.COMING_SOON  -> "Coming Soon"
        else                       -> null
    }
}

@Serializable data class FeatureEntry(val status: String)
@Serializable data class FeaturesResolveResponse(
    val features: Map<String, FeatureEntry>,
    val ttl: Long
)

// Form types
@Serializable data class FormResult(val success: Boolean, val message: String? = null)

// Waitlist types
@Serializable data class WaitlistResult(
    val success: Boolean,
    val position: Int? = null,
    val alreadyJoined: Boolean
)

// Auth types
data class OneloUser(
    val id: String,
    val email: String,
    val role: String,
    val tenantId: String,
)

data class OneloSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val user: OneloUser,
)

// Errors
class OneloApiException(message: String, val statusCode: Int) : Exception(message)
