package com.fredapps.gdriveshow.drive

import android.content.Context

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    fun startupFolder(): StartupFolder =
        StartupFolder(
            id = preferences.getString(KeyStartupFolderId, DriveRepository.RootFolderId) ?: DriveRepository.RootFolderId,
            title = preferences.getString(KeyStartupFolderTitle, "Drive Root") ?: "Drive Root",
        )

    fun setStartupFolder(folder: StartupFolder) {
        preferences.edit()
            .putString(KeyStartupFolderId, folder.id)
            .putString(KeyStartupFolderTitle, folder.title)
            .apply()
    }

    private companion object {
        const val PreferencesName = "gdrive_show_preferences"
        const val KeyStartupFolderId = "startup_folder_id"
        const val KeyStartupFolderTitle = "startup_folder_title"
    }
}

data class StartupFolder(
    val id: String,
    val title: String,
)

