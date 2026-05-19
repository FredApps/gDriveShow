package com.fredapps.gdriveshow.drive.auth

interface DriveTokenStore {
    fun read(): StoredDriveTokens?
    fun write(tokens: StoredDriveTokens)
    fun clear()
}

data class StoredDriveTokens(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtEpochSeconds: Long,
)

