package com.fredapps.gdriveshow.drive.auth

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GoogleDeviceCodeAuthClient(
    private val config: DriveAuthConfig,
    private val tokenStore: DriveTokenStore,
    private val clock: () -> Long = { System.currentTimeMillis() / 1000L },
) : DriveAuthClient {
    override fun startAuthorization(): DeviceAuthorizationStartResult {
        if (config.clientId.isBlank()) {
            return DeviceAuthorizationStartResult.Failed(
                "Missing Google OAuth TV client ID. Add it to google_oauth_tv_client_id.",
            )
        }

        return try {
            val response = postForm(
                url = DeviceCodeUrl,
                values = mapOf(
                    "client_id" to config.clientId,
                    "scope" to config.scopes.joinToString(" "),
                ),
            )

            if (!response.isSuccess) {
                return DeviceAuthorizationStartResult.Failed(response.errorMessage())
            }

            val json = JSONObject(response.body)
            DeviceAuthorizationStartResult.Prompt(
                DeviceAuthorizationPrompt(
                    deviceCode = json.getString("device_code"),
                    userCode = json.getString("user_code"),
                    verificationUrl = json.getString("verification_url"),
                    verificationUrlComplete = json.optString("verification_url_complete").ifBlank { null },
                    expiresInSeconds = json.getInt("expires_in"),
                    pollIntervalSeconds = json.optInt("interval", DefaultPollIntervalSeconds),
                ),
            )
        } catch (exception: IOException) {
            DeviceAuthorizationStartResult.Failed("Could not reach Google OAuth: ${exception.message}")
        } catch (exception: Exception) {
            DeviceAuthorizationStartResult.Failed("Could not start Drive authorization: ${exception.message}")
        }
    }

    override fun pollAuthorization(prompt: DeviceAuthorizationPrompt): DeviceAuthorizationResult {
        if (config.clientId.isBlank()) {
            return DeviceAuthorizationResult.Failed("Missing Google OAuth TV client ID.")
        }

        return try {
            val response = postForm(
                url = TokenUrl,
                values = mapOf(
                    "client_id" to config.clientId,
                    "device_code" to prompt.deviceCode,
                    "grant_type" to DeviceCodeGrantType,
                ),
            )
            val json = JSONObject(response.body)

            if (!response.isSuccess) {
                return when (json.optString("error")) {
                    "authorization_pending" -> DeviceAuthorizationResult.AuthorizationPending
                    "slow_down" -> DeviceAuthorizationResult.SlowDown
                    "access_denied" -> DeviceAuthorizationResult.Denied("The Google Drive authorization was denied.")
                    "expired_token" -> DeviceAuthorizationResult.Denied("The Google Drive authorization code expired.")
                    else -> DeviceAuthorizationResult.Failed(response.errorMessage(json))
                }
            }

            val expiresInSeconds = json.getInt("expires_in")
            val tokens = StoredDriveTokens(
                accessToken = json.getString("access_token"),
                refreshToken = json.optString("refresh_token").ifBlank { tokenStore.read()?.refreshToken },
                expiresAtEpochSeconds = clock() + expiresInSeconds,
            )
            tokenStore.write(tokens)

            DeviceAuthorizationResult.Authorized(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                expiresInSeconds = expiresInSeconds,
            )
        } catch (exception: IOException) {
            DeviceAuthorizationResult.Failed("Could not reach Google OAuth: ${exception.message}")
        } catch (exception: Exception) {
            DeviceAuthorizationResult.Failed("Could not poll Drive authorization: ${exception.message}")
        }
    }

    override fun signOut() {
        tokenStore.clear()
    }

    private fun postForm(url: String, values: Map<String, String>): OAuthHttpResponse {
        val encodedBody = values.entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }
        val bodyBytes = encodedBody.toByteArray(StandardCharsets.UTF_8)
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
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

        fun errorMessage(json: JSONObject? = null): String {
            val parsed = json ?: runCatching { JSONObject(body) }.getOrNull()
            val description = parsed?.optString("error_description")?.takeIf { it.isNotBlank() }
                ?: parsed?.optString("error")?.takeIf { it.isNotBlank() }
            return description ?: "Google OAuth returned HTTP $code."
        }
    }

    private companion object {
        const val DeviceCodeUrl = "https://oauth2.googleapis.com/device/code"
        const val TokenUrl = "https://oauth2.googleapis.com/token"
        const val DeviceCodeGrantType = "urn:ietf:params:oauth:grant-type:device_code"
        const val DefaultPollIntervalSeconds = 5
        const val TimeoutMillis = 15_000
    }
}

