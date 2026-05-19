package com.fredapps.gdriveshow.drive.auth

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class DriveAccessTokenProvider(
    private val config: DriveAuthConfig,
    private val tokenStore: DriveTokenStore,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000L },
) {
    fun accessToken(): AccessTokenResult {
        val storedTokens = tokenStore.read() ?: return AccessTokenResult.MissingTokens
        if (storedTokens.accessToken.isNotBlank() && storedTokens.expiresAtEpochSeconds > clock() + ExpiryLeewaySeconds) {
            return AccessTokenResult.Ready(storedTokens.accessToken)
        }

        val refreshToken = storedTokens.refreshToken
            ?: return AccessTokenResult.Failed("Google Drive token expired and no refresh token is available.")

        return try {
            val response = postForm(
                values = mapOf(
                    "client_id" to config.clientId,
                    "refresh_token" to refreshToken,
                    "grant_type" to "refresh_token",
                ),
            )
            val json = JSONObject(response.body)

            if (!response.isSuccess) {
                return AccessTokenResult.Failed(response.errorMessage(json))
            }

            val expiresInSeconds = json.getInt("expires_in")
            val refreshedTokens = StoredDriveTokens(
                accessToken = json.getString("access_token"),
                refreshToken = refreshToken,
                expiresAtEpochSeconds = clock() + expiresInSeconds,
            )
            tokenStore.write(refreshedTokens)
            AccessTokenResult.Ready(refreshedTokens.accessToken)
        } catch (exception: IOException) {
            AccessTokenResult.Failed("Could not refresh Google Drive token: ${exception.message}")
        } catch (exception: Exception) {
            AccessTokenResult.Failed("Could not read Google Drive token: ${exception.message}")
        }
    }

    private fun postForm(values: Map<String, String>): OAuthHttpResponse {
        val encodedBody = values.entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }
        val bodyBytes = encodedBody.toByteArray(StandardCharsets.UTF_8)
        val connection = (URL(TokenUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TimeoutMillis
            readTimeout = TimeoutMillis
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Length", bodyBytes.size.toString())
        }

        return try {
            connection.outputStream.use { it.write(bodyBytes) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            OAuthHttpResponse(
                code = code,
                body = stream?.bufferedReader()?.use { it.readText() }.orEmpty(),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private data class OAuthHttpResponse(
        val code: Int,
        val body: String,
    ) {
        val isSuccess: Boolean = code in 200..299

        fun errorMessage(json: JSONObject): String =
            json.optString("error_description").takeIf { it.isNotBlank() }
                ?: json.optString("error").takeIf { it.isNotBlank() }
                ?: "Google OAuth returned HTTP $code."
    }

    private companion object {
        const val TokenUrl = "https://oauth2.googleapis.com/token"
        const val TimeoutMillis = 15_000
        const val ExpiryLeewaySeconds = 60L
    }
}

sealed interface AccessTokenResult {
    data class Ready(val accessToken: String) : AccessTokenResult
    data object MissingTokens : AccessTokenResult
    data class Failed(val message: String) : AccessTokenResult
}

