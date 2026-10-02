package com.example.data.repository

import com.example.data.local.dao.TimetableDao
import com.example.data.local.entity.TimetableSlotEntity
import kotlinx.coroutines.flow.Flow

class TimetableRepository(private val timetableDao: TimetableDao) {

    val allSlots: Flow<List<TimetableSlotEntity>> = timetableDao.getAllSlots()

    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlotEntity>> = timetableDao.getSlotsForDay(dayOfWeek)

    suspend fun getSlotsForDaySync(dayOfWeek: Int): List<TimetableSlotEntity> = timetableDao.getSlotsForDaySync(dayOfWeek)

    fun getSlotsForSubject(subjectId: Long): Flow<List<TimetableSlotEntity>> = timetableDao.getSlotsForSubject(subjectId)

    suspend fun getSlotById(id: Long): TimetableSlotEntity? = timetableDao.getSlotById(id)

    suspend fun insertSlot(slot: TimetableSlotEntity): Long = timetableDao.insertSlot(slot)

    suspend fun insertSlots(slots: List<TimetableSlotEntity>): List<Long> = timetableDao.insertSlots(slots)

    suspend fun updateSlot(slot: TimetableSlotEntity) = timetableDao.updateSlot(slot)

    suspend fun deleteSlot(slot: TimetableSlotEntity) = timetableDao.deleteSlot(slot)

    suspend fun deleteSlotById(id: Long) = timetableDao.deleteSlotById(id)

    suspend fun deleteAllSlots() = timetableDao.deleteAllSlots()
}
