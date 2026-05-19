package com.fredapps.gdriveshow.drive

sealed interface DriveConnectionState {
    data object Disconnected : DriveConnectionState
    data object Connecting : DriveConnectionState
    data class Connected(val accountLabel: String) : DriveConnectionState
    data class Failed(val message: String) : DriveConnectionState
}

sealed interface DriveContentState {
    data object Loading : DriveContentState
    data object Empty : DriveContentState
    data class Ready(val items: List<DriveItem>) : DriveContentState
    data class Failed(val message: String) : DriveContentState
}

val DriveConnectionState.statusLabel: String
    get() = when (this) {
        DriveConnectionState.Disconnected -> "Google Drive not connected"
        DriveConnectionState.Connecting -> "Connecting to Google Drive"
        is DriveConnectionState.Connected -> accountLabel
        is DriveConnectionState.Failed -> "Drive connection failed"
    }

