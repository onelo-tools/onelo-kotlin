# onelo-kotlin

Official Kotlin SDK for [Onelo](https://onelo.tools), targeting **server-side / JVM 17+ workloads** — features, paywalls, forms, and waitlists.

> **For Android apps, use [`onelo-android`](https://github.com/onelo-tools/onelo-android) instead.**
>
> `onelo-kotlin` is a lightweight client built for backends and CLIs. It does not include
> the security primitives required for client-side mobile apps: no Play Integrity / App Attest,
> no PKCE, no encrypted token storage, and no end-user authentication flow. Sessions are held
> in memory only. Run it inside a trusted server environment, never bundled into a phone app.

## Installation

```kotlin
// build.gradle.kts
implementation("io.onelo:sdk:1.0.0")
```

Install from GitHub:
```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}
dependencies {
    implementation("com.github.onelo-tools:onelo-kotlin:v1.0.0")
}
```

## Quick Start

```kotlin
val onelo = Onelo(publishableKey = "pk_live_...")

// Set user context after login (suspend function)
onelo.identify(currentUser.id, plan = "pro")

// Features
if (onelo.features.isEnabled("export-button")) {
    showExportButton()
}

// Gating — use onelo.features.isEnabled(name) (resolved server-side against the
// user's real plan); onelo.features.identify(userId, plan) sets the plan.

// Forms
val result = onelo.forms.submit("feedback", mapOf("message" to "Great app!"))

// Waitlist
val joined = onelo.waitlist.join("beta", email = "user@example.com")
```

## Modules

| Module | Class | Description |
|--------|-------|-------------|
| `onelo.features` | `OneloFeatures` | Feature flags — `isEnabled()`, `status()` |
| `onelo.forms` | `OneloForms` | Form submission — `submit()` |
| `onelo.waitlist` | `OneloWaitlist` | Waitlist signup — `join()` |

## Requirements

- Kotlin 1.9+
- JVM 17+ (server-side / CLI usage only)
- kotlinx.coroutines (all network calls are `suspend` functions)

## When to use which Kotlin SDK?

| Use case | Package |
|---|---|
| Android app talking to the Onelo API directly from the device | [`onelo-android`](https://github.com/onelo-tools/onelo-android) |
| Kotlin/JVM backend service calling the Onelo API on behalf of users | `onelo-kotlin` (this package) |
| CLI tool, build script, or one-off integration | `onelo-kotlin` |

## License

MIT
