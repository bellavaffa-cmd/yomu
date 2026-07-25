package com.yomu.reader.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.yomu.reader.ui.browse.BrowseScreen
import com.yomu.reader.ui.browse.SourceBrowseScreen
import com.yomu.reader.ui.detail.MangaDetailScreen
import com.yomu.reader.ui.extension.ExtensionReposScreen
import com.yomu.reader.ui.extension.ExtensionsScreen
import com.yomu.reader.ui.history.HistoryScreen
import com.yomu.reader.ui.library.LibraryScreen
import com.yomu.reader.ui.more.MoreScreen
import com.yomu.reader.ui.navigation.Routes
import com.yomu.reader.ui.navigation.TopLevelDestination
import com.yomu.reader.ui.reader.ReaderScreen
import com.yomu.reader.ui.updates.UpdatesScreen

@Composable
fun YomuApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = currentRoute in TopLevelDestination.entries.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(iconFor(dest), contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Routes.LIBRARY) {
                LibraryScreen(padding, onMangaClick = { navController.navigate(Routes.detail(it)) })
            }
            composable(Routes.UPDATES) {
                UpdatesScreen(padding, onChapterClick = { m, c -> navController.navigate(Routes.reader(m, c)) })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(padding, onResumeClick = { m, c -> navController.navigate(Routes.reader(m, c)) })
            }
            composable(Routes.BROWSE) {
                BrowseScreen(
                    padding,
                    onSourceClick = { navController.navigate(Routes.sourceBrowse(it)) },
                    onExtensionsClick = { navController.navigate(Routes.EXTENSIONS) },
                )
            }
            composable(Routes.EXTENSIONS) {
                ExtensionsScreen(
                    onBack = { navController.popBackStack() },
                    onManageRepos = { navController.navigate(Routes.EXTENSION_REPOS) },
                )
            }
            composable(Routes.EXTENSION_REPOS) {
                ExtensionReposScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.MORE) {
                MoreScreen(padding)
            }

            composable(
                Routes.SOURCE_BROWSE,
                arguments = listOf(navArgument("sourceId") { type = NavType.LongType }),
            ) { entry ->
                val sourceId = entry.arguments?.getLong("sourceId") ?: return@composable
                SourceBrowseScreen(
                    sourceId = sourceId,
                    onBack = { navController.popBackStack() },
                    onMangaClick = { navController.navigate(Routes.detail(it)) },
                )
            }

            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("mangaId") { type = NavType.LongType }),
            ) { entry ->
                val mangaId = entry.arguments?.getLong("mangaId") ?: return@composable
                MangaDetailScreen(
                    mangaId = mangaId,
                    onBack = { navController.popBackStack() },
                    onChapterClick = { m, c -> navController.navigate(Routes.reader(m, c)) },
                )
            }

            composable(
                Routes.READER,
                arguments = listOf(
                    navArgument("mangaId") { type = NavType.LongType },
                    navArgument("chapterId") { type = NavType.LongType },
                ),
            ) { entry ->
                val mangaId = entry.arguments?.getLong("mangaId") ?: return@composable
                val chapterId = entry.arguments?.getLong("chapterId") ?: return@composable
                ReaderScreen(mangaId = mangaId, chapterId = chapterId, onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun iconFor(dest: TopLevelDestination): ImageVector = when (dest) {
    TopLevelDestination.LIBRARY -> Icons.Filled.CollectionsBookmark
    TopLevelDestination.UPDATES -> Icons.Filled.NewReleases
    TopLevelDestination.HISTORY -> Icons.Filled.History
    TopLevelDestination.BROWSE -> Icons.Filled.Explore
    TopLevelDestination.MORE -> Icons.Filled.MoreHoriz
}
