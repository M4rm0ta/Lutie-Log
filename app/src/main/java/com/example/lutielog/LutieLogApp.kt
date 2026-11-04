package com.example.lutielog

import android.app.Application
import com.example.lutielog.data.AppDatabase
import com.example.lutielog.data.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LutieLogApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        WorkoutRepository(
            exerciseDao = database.exerciseDao(),
            workoutDao = database.workoutDao(),
            bodyWeightDao = database.bodyWeightDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        CrashCollector.install(this)
        // 收尾上次遗留的未结束会话（补全时间/训练量、清理空会话），幂等
        appScope.launch {
            runCatching { repository.reconcileSessions() }
        }
    }
}
