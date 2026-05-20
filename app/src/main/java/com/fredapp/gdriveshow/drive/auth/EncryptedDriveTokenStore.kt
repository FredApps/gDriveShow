package com.fredapp.gdriveshow.drive.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedDriveTokenStore(context: Context) : DriveTokenStore {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        PreferencesName,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun read(): StoredDriveTokens? {
        val accessToken = preferences.getString(KeyAccessToken, null) ?: return null
        return StoredDriveTokens(
            accessToken = accessToken,
            refreshToken = preferences.getString(KeyRefreshToken, null),
            expiresAtEpochSeconds = preferences.getLong(KeyExpiresAt, 0L),
        )
    }

    override fun write(tokens: StoredDriveTokens) {
        preferences.edit()
            .putString(KeyAccessToken, tokens.accessToken)
            .putString(KeyRefreshToken, tokens.refreshToken)
            .putLong(KeyExpiresAt, tokens.expiresAtEpochSeconds)
            .apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PreferencesName = "gdrive_show_drive_tokens"
        const val KeyAccessToken = "access_token"
        const val KeyRefreshToken = "refresh_token"
        const val KeyExpiresAt = "expires_at_epoch_seconds"
    }
}

