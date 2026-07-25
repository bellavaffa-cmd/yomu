package com.yomu.reader.ui.navigation

object Routes {
    const val LIBRARY = "library"
    const val UPDATES = "updates"
    const val HISTORY = "history"
    const val BROWSE = "browse"
    const val MORE = "more"

    const val EXTENSIONS = "extensions"
    const val EXTENSION_REPOS = "extension_repos"
    const val DOWNLOADS = "downloads"

    const val SOURCE_BROWSE = "source/{sourceId}"
    fun sourceBrowse(sourceId: Long) = "source/$sourceId"

    const val DETAIL = "manga/{mangaId}"
    fun detail(mangaId: Long) = "manga/$mangaId"

    const val READER = "reader/{mangaId}/{chapterId}"
    fun reader(mangaId: Long, chapterId: Long) = "reader/$mangaId/$chapterId"
}

/** Top-level destinations shown in the bottom navigation bar. */
enum class TopLevelDestination(val route: String, val label: String) {
    LIBRARY(Routes.LIBRARY, "Library"),
    UPDATES(Routes.UPDATES, "Updates"),
    HISTORY(Routes.HISTORY, "History"),
    BROWSE(Routes.BROWSE, "Browse"),
    MORE(Routes.MORE, "More"),
}
