package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY date DESC, startTime ASC")
    fun getAllRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY startTime ASC, periodNumber ASC")
    fun getRecordsForDate(date: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY startTime ASC, periodNumber ASC")
    suspend fun getRecordsForDateSync(date: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    fun getRecordsBetweenDates(startDate: String, endDate: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startTime ASC")
    suspend fun getRecordsBetweenDatesSync(startDate: String, endDate: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE id = :id")
    suspend fun getRecordById(id: Long): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE date = :date AND subjectId = :subjectId AND periodNumber = :periodNumber LIMIT 1")
    suspend fun getRecordForSlot(date: String, subjectId: Long, periodNumber: Int): AttendanceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>): List<Long>

    @Update
    suspend fun updateRecord(record: AttendanceRecordEntity)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecordEntity)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM attendance_records WHERE date = :date")
    suspend fun deleteRecordsForDate(date: String)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllRecords()
}
