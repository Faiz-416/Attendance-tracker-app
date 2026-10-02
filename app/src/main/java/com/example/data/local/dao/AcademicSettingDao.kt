package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AcademicSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicSettingDao {
    @Query("SELECT * FROM academic_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AcademicSettingEntity?>

    @Query("SELECT * FROM academic_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AcademicSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: AcademicSettingEntity)
}
