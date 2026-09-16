package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.ScreenshotEntity
import com.example.data.model.ScreenshotTagCrossRef
import com.example.data.model.TagEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ScreenshotEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        ScreenshotTagCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun screenshotDao(): ScreenshotDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(id = "shopping", name = "购物", icon = "🛒", sort = 1),
            CategoryEntity(id = "chat", name = "聊天", icon = "💬", sort = 2),
            CategoryEntity(id = "work", name = "工作", icon = "💼", sort = 3),
            CategoryEntity(id = "study", name = "学习", icon = "📚", sort = 4),
            CategoryEntity(id = "game", name = "游戏", icon = "🎮", sort = 5),
            CategoryEntity(id = "travel", name = "旅行", icon = "✈️", sort = 6),
            CategoryEntity(id = "finance", name = "消费", icon = "💰", sort = 7),
            CategoryEntity(id = "express", name = "快递", icon = "📦", sort = 8),
            CategoryEntity(id = "social", name = "社交", icon = "📱", sort = 9),
            CategoryEntity(id = "doc", name = "资料", icon = "📄", sort = 10),
            CategoryEntity(id = "location", name = "地址", icon = "📍", sort = 11),
            CategoryEntity(id = "order", name = "订单", icon = "🔢", sort = 12),
            CategoryEntity(id = "web", name = "网页", icon = "🔗", sort = 13),
            CategoryEntity(id = "other", name = "其他", icon = "📷", sort = 14)
        )

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "screenshot_manager.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).categoryDao().insertCategories(DEFAULT_CATEGORIES)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
