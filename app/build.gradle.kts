import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val oauthLocalProperties = Properties()
val oauthLocalFile = rootProject.file("oauth.local.properties")
if (oauthLocalFile.isFile) {
    oauthLocalFile.inputStream().use { oauthLocalProperties.load(it) }
}

val googleOauthTvClientId = providers.environmentVariable("GOOGLE_OAUTH_TV_CLIENT_ID")
    .orElse(providers.gradleProperty("googleOAuthTvClientId"))
    .orElse(oauthLocalProperties.getProperty("googleOAuthTvClientId") ?: "")

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.fredapp.gdriveshow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fredapp.gdriveshow"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField(
            "String",
            "GOOGLE_OAUTH_TV_CLIENT_ID",
            googleOauthTvClientId.get().trim().asBuildConfigString(),
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val keystorePath = providers.environmentVariable("GDRIVESHOW_KEYSTORE").orNull
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("GDRIVESHOW_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("GDRIVESHOW_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("GDRIVESHOW_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            val keystorePath = providers.environmentVariable("GDRIVESHOW_KEYSTORE").orNull
            if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
