package com.example.lutielog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lutielog.data.dao.BodyWeightDao
import com.example.lutielog.data.dao.ExerciseDao
import com.example.lutielog.data.dao.WorkoutDao
import com.example.lutielog.data.entity.BodyWeight
import com.example.lutielog.data.entity.Exercise
import com.example.lutielog.data.entity.WorkoutSession
import com.example.lutielog.data.entity.WorkoutSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Exercise::class, WorkoutSession::class, WorkoutSet::class, BodyWeight::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyWeightDao(): BodyWeightDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v1 -> v2: 新增 recordType, durationSec 字段
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise ADD COLUMN recordType TEXT NOT NULL DEFAULT 'weight'")
                db.execSQL("ALTER TABLE workout_set ADD COLUMN durationSec INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_set ADD COLUMN recordType TEXT NOT NULL DEFAULT 'weight'")
            }
        }

        // v2 -> v3: 把时间型动作标记为 time
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE exercise SET recordType = 'time' WHERE name IN ('平板支撑')")
            }
        }

        // v3 -> v4: 扩充内置动作库，按名字幂等补全
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                DefaultExercises.all().forEach { e ->
                    db.execSQL(
                        """
                        INSERT INTO exercise
                            (name, muscleGroup, type, unit, recordType, isCustom, isFavorite, sortOrder, createdAt, updatedAt)
                        SELECT ?, ?, ?, ?, ?, 0, 0, ?, ?, ?
                        WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = ?)
                        """.trimIndent(),
                        arrayOf<Any>(e.name, e.muscleGroup, e.type, e.unit, e.recordType, e.sortOrder, now, now, e.name)
                    )
                }
            }
        }

        // v4 -> v5: 动作记录模式/动态字段 + 组多指标列；回填内置动作模板并幂等补全
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise ADD COLUMN trackMode TEXT NOT NULL DEFAULT 'sets'")
                db.execSQL("ALTER TABLE exercise ADD COLUMN metrics TEXT NOT NULL DEFAULT 'weight,reps'")
                db.execSQL("ALTER TABLE workout_set ADD COLUMN distanceM REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_set ADD COLUMN speed REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_set ADD COLUMN incline REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_set ADD COLUMN resistanceLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_set ADD COLUMN calories INTEGER NOT NULL DEFAULT 0")

                val now = System.currentTimeMillis()
                DefaultExercises.all().forEach { e ->
                    // 已有同名内置动作：更新到最新记录模板（isCustom=0，避免覆盖用户自定义动作）
                    db.execSQL(
                        "UPDATE exercise SET muscleGroup=?, type=?, recordType=?, trackMode=?, metrics=?, sortOrder=? WHERE name=? AND isCustom=0",
                        arrayOf<Any>(e.muscleGroup, e.type, e.recordType, e.trackMode, e.metrics, e.sortOrder, e.name)
                    )
                    // 缺失动作补全
                    db.execSQL(
                        """
                        INSERT INTO exercise
                            (name, muscleGroup, type, unit, recordType, trackMode, metrics,
                             isCustom, isFavorite, sortOrder, createdAt, updatedAt)
                        SELECT ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?
                        WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = ?)
                        """.trimIndent(),
                        arrayOf<Any>(
                            e.name, e.muscleGroup, e.type, e.unit, e.recordType, e.trackMode, e.metrics,
                            e.sortOrder, now, now, e.name
                        )
                    )
                }
            }
        }

        // v5 -> v6: 扩充内置动作库（高位下拉各握把等变式），按名字幂等补全
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                DefaultExercises.all().forEach { e ->
                    db.execSQL(
                        "UPDATE exercise SET muscleGroup=?, type=?, recordType=?, trackMode=?, metrics=?, sortOrder=? WHERE name=? AND isCustom=0",
                        arrayOf<Any>(e.muscleGroup, e.type, e.recordType, e.trackMode, e.metrics, e.sortOrder, e.name)
                    )
                    db.execSQL(
                        """
                        INSERT INTO exercise
                            (name, muscleGroup, type, unit, recordType, trackMode, metrics,
                             isCustom, isFavorite, sortOrder, createdAt, updatedAt)
                        SELECT ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?
                        WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = ?)
                        """.trimIndent(),
                        arrayOf<Any>(
                            e.name, e.muscleGroup, e.type, e.unit, e.recordType, e.trackMode, e.metrics,
                            e.sortOrder, now, now, e.name
                        )
                    )
                }
            }
        }

        // v6 -> v7: 动作单独休息时间、组类型、体重记录表
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise ADD COLUMN restSec INTEGER NOT NULL DEFAULT 90")
                db.execSQL("ALTER TABLE workout_set ADD COLUMN setType TEXT NOT NULL DEFAULT 'normal'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS body_weight (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        weightKg REAL NOT NULL,
                        bodyFatPct REAL NOT NULL,
                        note TEXT NOT NULL,
                        measuredAt INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // v7 -> v8: 扩充内置动作库（健身房/自重/弹力带/臀部变式），按名字幂等补全
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                DefaultExercises.all().forEach { e ->
                    // 已有同名内置动作：更新分类与排序（不覆盖用户自定义动作）
                    db.execSQL(
                        "UPDATE exercise SET muscleGroup=?, type=?, recordType=?, trackMode=?, metrics=?, sortOrder=? WHERE name=? AND isCustom=0",
                        arrayOf<Any>(e.muscleGroup, e.type, e.recordType, e.trackMode, e.metrics, e.sortOrder, e.name)
                    )
                    // 缺失动作补全（含 restSec 列）
                    db.execSQL(
                        """
                        INSERT INTO exercise
                            (name, muscleGroup, type, unit, recordType, trackMode, metrics, restSec,
                             isCustom, isFavorite, sortOrder, createdAt, updatedAt)
                        SELECT ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?
                        WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = ?)
                        """.trimIndent(),
                        arrayOf<Any>(
                            e.name, e.muscleGroup, e.type, e.unit, e.recordType, e.trackMode, e.metrics,
                            e.restSec, e.sortOrder, now, now, e.name
                        )
                    )
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lutielog_db"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    prepopulateExercises(database.exerciseDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun prepopulateExercises(dao: ExerciseDao) {
            DefaultExercises.all().forEach { dao.insert(it) }
        }
    }
}
