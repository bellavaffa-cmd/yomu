package com.yomu.reader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MangaEntity::class,
        ChapterEntity::class,
        HistoryEntity::class,
        CategoryEntity::class,
        MangaCategoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class YomuDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao
    abstract fun chapterDao(): ChapterDao
    abstract fun historyDao(): HistoryDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: YomuDatabase? = null

        fun get(context: Context): YomuDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    YomuDatabase::class.java,
                    "yomu.db",
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
