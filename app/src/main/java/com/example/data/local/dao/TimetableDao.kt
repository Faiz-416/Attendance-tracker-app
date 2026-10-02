package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.TimetableSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllSlots(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC, periodNumber ASC")
    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC, periodNumber ASC")
    suspend fun getSlotsForDaySync(dayOfWeek: Int): List<TimetableSlotEntity>

    @Query("SELECT * FROM timetable_slots WHERE subjectId = :subjectId")
    fun getSlotsForSubject(subjectId: Long): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE id = :id")
    suspend fun getSlotById(id: Long): TimetableSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<TimetableSlotEntity>): List<Long>

    @Update
    suspend fun updateSlot(slot: TimetableSlotEntity)

    @Delete
    suspend fun deleteSlot(slot: TimetableSlotEntity)

    @Query("DELETE FROM timetable_slots WHERE id = :id")
    suspend fun deleteSlotById(id: Long)

    @Query("DELETE FROM timetable_slots")
    suspend fun deleteAllSlots()
}
