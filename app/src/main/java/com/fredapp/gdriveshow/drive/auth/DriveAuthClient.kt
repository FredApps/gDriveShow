package com.fredapp.gdriveshow.drive.auth

interface DriveAuthClient {
    fun startAuthorization(): DeviceAuthorizationStartResult
    fun pollAuthorization(prompt: DeviceAuthorizationPrompt): DeviceAuthorizationResult
    fun signOut()
}

data class DeviceAuthorizationPrompt(
    val deviceCode: String,
    val userCode: String,
    val verificationUrl: String,
    val verificationUrlComplete: String?,
    val expiresInSeconds: Int,
    val pollIntervalSeconds: Int,
)

sealed interface DeviceAuthorizationStartResult {
    data class Prompt(val prompt: DeviceAuthorizationPrompt) : DeviceAuthorizationStartResult
    data class Failed(val message: String) : DeviceAuthorizationStartResult
}

sealed interface DeviceAuthorizationResult {
    data object AuthorizationPending : DeviceAuthorizationResult
    data object SlowDown : DeviceAuthorizationResult
    data class Authorized(
        val accessToken: String,
        val refreshToken: String?,
        val expiresInSeconds: Int,
    ) : DeviceAuthorizationResult

    data class Denied(val message: String) : DeviceAuthorizationResult
    data class Failed(val message: String) : DeviceAuthorizationResult
}
