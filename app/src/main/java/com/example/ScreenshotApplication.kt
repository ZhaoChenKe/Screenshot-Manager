package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ScreenshotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ScreenshotApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var repository: ScreenshotRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)
        repository = ScreenshotRepository(
            context = this,
            database = database,
            preferencesRepository = preferencesRepository
        )
    }

    companion object {
        lateinit var instance: ScreenshotApplication
            private set
    }
}
