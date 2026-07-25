package com.yomu.reader.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.yomu.reader.YomuApplication
import com.yomu.reader.data.AppPreferences
import com.yomu.reader.data.ExtensionRepoStore
import com.yomu.reader.data.MangaRepository
import com.yomu.reader.extension.ExtensionManager
import com.yomu.reader.sync.DriveSyncManager
import com.yomu.reader.update.UpdateManager

@Composable
private fun app(): YomuApplication =
    LocalContext.current.applicationContext as YomuApplication

/** Pull the app-wide repository out of the Application container. */
@Composable
fun rememberRepository(): MangaRepository = app().repository

@Composable
fun rememberExtensionManager(): ExtensionManager = app().extensionManager

@Composable
fun rememberExtensionRepoStore(): ExtensionRepoStore = app().extensionRepoStore

@Composable
fun rememberAppPreferences(): AppPreferences = app().appPreferences

@Composable
fun rememberUpdateManager(): UpdateManager = app().updateManager

@Composable
fun rememberDriveSyncManager(): DriveSyncManager = app().driveSyncManager
