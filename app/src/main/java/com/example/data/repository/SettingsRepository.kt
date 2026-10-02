package com.example.data.repository

import com.example.data.local.dao.AcademicSettingDao
import com.example.data.local.entity.AcademicSettingEntity
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val academicSettingDao: AcademicSettingDao) {

    val settings: Flow<AcademicSettingEntity?> = academicSettingDao.getSettings()

    suspend fun getSettingsSync(): AcademicSettingEntity? = academicSettingDao.getSettingsSync()

    suspend fun saveSettings(settings: AcademicSettingEntity) = academicSettingDao.insertOrUpdate(settings)
}
