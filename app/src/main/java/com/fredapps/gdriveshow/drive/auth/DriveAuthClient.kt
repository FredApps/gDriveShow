package com.fredapps.gdriveshow.drive.auth

interface DriveAuthClient {
    fun startAuthorization(): DeviceAuthorizationPrompt
    fun pollAuthorization(prompt: DeviceAuthorizationPrompt): DeviceAuthorizationResult
    fun signOut()
}

data class DeviceAuthorizationPrompt(
    val deviceCode: String,
    val userCode: String,
    val verificationUrl: String,
    val expiresInSeconds: Int,
    val pollIntervalSeconds: Int,
)

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

