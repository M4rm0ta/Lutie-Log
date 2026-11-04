package com.example.lutielog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.lutielog.data.entity.WorkoutSession
import com.example.lutielog.data.entity.WorkoutSet
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // 训练会话
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Query("SELECT * FROM workout_session ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun getSessionById(id: Long): WorkoutSession?

    @Query("DELETE FROM workout_session WHERE id = :id")
    suspend fun deleteSession(id: Long)

    // 遗留会话收尾：有训练组但 endTime 为空（用户上次没点"结束本次训练"），
    // 用最后一组的时间补全结束时刻、时长，并由各组现算总训练量。幂等，只影响未结束会话。
    @Query(
        """
        UPDATE workout_session
        SET endTime = (SELECT MAX(createdAt) FROM workout_set WHERE sessionId = workout_session.id),
            durationSec = (SELECT CAST((MAX(createdAt) - workout_session.startTime) / 1000 AS INTEGER)
                           FROM workout_set WHERE sessionId = workout_session.id),
            totalVolume = (SELECT COALESCE(SUM(weightKg * reps), 0)
                           FROM workout_set WHERE sessionId = workout_session.id AND setType != 'warmup'),
            updatedAt = :now
        WHERE endTime IS NULL
          AND EXISTS (SELECT 1 FROM workout_set WHERE sessionId = workout_session.id)
        """
    )
    suspend fun reconcileOpenSessions(now: Long)

    // 清理没有任何训练组的空会话（点了"开始训练"又取消产生的脏数据）
    @Query(
        """
        DELETE FROM workout_session
        WHERE endTime IS NULL
          AND NOT EXISTS (SELECT 1 FROM workout_set WHERE sessionId = workout_session.id)
        """
    )
    suspend fun deleteEmptySessions()

    // 训练组
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSet): Long

    @Query("DELETE FROM workout_set WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("SELECT * FROM workout_set WHERE sessionId = :sessionId ORDER BY exerciseName ASC, setIndex ASC")
    fun getSetsBySession(sessionId: Long): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_set WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getSetsBySessionOnce(sessionId: Long): List<WorkoutSet>

    @Query("SELECT * FROM workout_set ORDER BY id ASC")
    fun getAllSets(): Flow<List<WorkoutSet>>

    @Query("SELECT COUNT(*) FROM workout_set WHERE sessionId = :sessionId")
    suspend fun getSetCountBySession(sessionId: Long): Int

    // 取某动作最近一次正式组（用于开练时预填重量/次数）
    @Query("SELECT * FROM workout_set WHERE exerciseId = :exerciseId AND setType = 'normal' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastNormalSetForExercise(exerciseId: Long): WorkoutSet?

    // 取某动作最近一次任意组（没有正式组时兜底）
    @Query("SELECT * FROM workout_set WHERE exerciseId = :exerciseId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastSetForExercise(exerciseId: Long): WorkoutSet?

    @Transaction
    suspend fun finishSession(sessionId: Long, endTime: Long) {
        val session = getSessionById(sessionId) ?: return
        val sets = getSetsBySessionOnce(sessionId)
        val durationSec = ((endTime - session.startTime) / 1000).toInt()
        // 热身组不计入训练量
        val totalVolume = sets.filter { it.setType != "warmup" }.sumOf { it.weightKg * it.reps }
        updateSession(
            session.copy(
                endTime = endTime,
                durationSec = durationSec,
                totalVolume = totalVolume,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
