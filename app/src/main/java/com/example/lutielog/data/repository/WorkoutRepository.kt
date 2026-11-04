package com.example.lutielog.data.repository

import com.example.lutielog.data.dao.BodyWeightDao
import com.example.lutielog.data.dao.ExerciseDao
import com.example.lutielog.data.dao.WorkoutDao
import com.example.lutielog.data.entity.BodyWeight
import com.example.lutielog.data.entity.Exercise
import com.example.lutielog.data.entity.WorkoutSession
import com.example.lutielog.data.entity.WorkoutSet
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao,
    private val bodyWeightDao: BodyWeightDao
) {
    // 动作
    val allExercises: Flow<List<Exercise>> = exerciseDao.getAllExercises()

    fun getExercisesByMuscleGroup(group: String): Flow<List<Exercise>> =
        exerciseDao.getExercisesByMuscleGroup(group)

    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getExerciseById(id)

    suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.insert(exercise)

    suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)

    suspend fun deleteExercise(id: Long) = exerciseDao.delete(id)

    // 训练会话
    val allSessions: Flow<List<WorkoutSession>> = workoutDao.getAllSessions()

    suspend fun startSession(): Long {
        val session = WorkoutSession(startTime = System.currentTimeMillis())
        return workoutDao.insertSession(session)
    }

    suspend fun finishSession(sessionId: Long) {
        workoutDao.finishSession(sessionId, System.currentTimeMillis())
    }

    suspend fun getSessionById(id: Long): WorkoutSession? = workoutDao.getSessionById(id)

    suspend fun deleteSession(id: Long) = workoutDao.deleteSession(id)

    /**
     * 启动时收尾：把上次未正常结束、但已记录训练组的会话补全结束时间/时长/训练量，
     * 并清理没有任何组的空会话。保证历史与统计不依赖用户是否点了"结束本次训练"。
     */
    suspend fun reconcileSessions() {
        workoutDao.reconcileOpenSessions(System.currentTimeMillis())
        workoutDao.deleteEmptySessions()
    }

    // 训练组
    fun getSetsBySession(sessionId: Long): Flow<List<WorkoutSet>> =
        workoutDao.getSetsBySession(sessionId)

    val allSets: Flow<List<WorkoutSet>> = workoutDao.getAllSets()

    suspend fun getSetsBySessionOnce(sessionId: Long): List<WorkoutSet> =
        workoutDao.getSetsBySessionOnce(sessionId)

    suspend fun insertSet(set: WorkoutSet): Long = workoutDao.insertSet(set)

    suspend fun deleteSet(id: Long) = workoutDao.deleteSet(id)

    suspend fun getSetCount(sessionId: Long): Int = workoutDao.getSetCountBySession(sessionId)

    /** 预填：取某动作最近一次正式组（无则最近任意组），用于开练带出重量/次数 */
    suspend fun getPrefillSet(exerciseId: Long): WorkoutSet? =
        workoutDao.getLastNormalSetForExercise(exerciseId)
            ?: workoutDao.getLastSetForExercise(exerciseId)

    // 体重记录
    val allBodyWeights: Flow<List<BodyWeight>> = bodyWeightDao.getAll()

    suspend fun insertBodyWeight(record: BodyWeight): Long = bodyWeightDao.insert(record)

    suspend fun deleteBodyWeight(id: Long) = bodyWeightDao.delete(id)
}
