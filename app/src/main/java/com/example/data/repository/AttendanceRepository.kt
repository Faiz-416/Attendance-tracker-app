package com.example.data.repository

import com.example.data.local.dao.AttendanceDao
import com.example.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val attendanceDao: AttendanceDao) {

    val allRecords: Flow<List<AttendanceRecordEntity>> = attendanceDao.getAllRecords()

    fun getRecordsForDate(date: String): Flow<List<AttendanceRecordEntity>> = attendanceDao.getRecordsForDate(date)

    suspend fun getRecordsForDateSync(date: String): List<AttendanceRecordEntity> = attendanceDao.getRecordsForDateSync(date)

    fun getRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecordEntity>> = attendanceDao.getRecordsForSubject(subjectId)

    fun getRecordsBetweenDates(startDate: String, endDate: String): Flow<List<AttendanceRecordEntity>> =
        attendanceDao.getRecordsBetweenDates(startDate, endDate)

    suspend fun getRecordsBetweenDatesSync(startDate: String, endDate: String): List<AttendanceRecordEntity> =
        attendanceDao.getRecordsBetweenDatesSync(startDate, endDate)

    suspend fun getRecordById(id: Long): AttendanceRecordEntity? = attendanceDao.getRecordById(id)

    suspend fun getRecordForSlot(date: String, subjectId: Long, periodNumber: Int): AttendanceRecordEntity? =
        attendanceDao.getRecordForSlot(date, subjectId, periodNumber)

    suspend fun insertRecord(record: AttendanceRecordEntity): Long = attendanceDao.insertRecord(record)

    suspend fun insertRecords(records: List<AttendanceRecordEntity>): List<Long> = attendanceDao.insertRecords(records)

    suspend fun updateRecord(record: AttendanceRecordEntity) = attendanceDao.updateRecord(record)

    suspend fun deleteRecord(record: AttendanceRecordEntity) = attendanceDao.deleteRecord(record)

    suspend fun deleteRecordById(id: Long) = attendanceDao.deleteRecordById(id)

    suspend fun deleteRecordsForDate(date: String) = attendanceDao.deleteRecordsForDate(date)

    suspend fun deleteAllRecords() = attendanceDao.deleteAllRecords()
}
