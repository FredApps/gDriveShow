package com.fredapps.gdriveshow.drive.auth

data class DriveAuthConfig(
    val clientId: String,
    val scopes: List<String> = listOf(DriveScopes.ReadOnly),
)

object DriveScopes {
    const val ReadOnly = "https://www.googleapis.com/auth/drive.readonly"
}

