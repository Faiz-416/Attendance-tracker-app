package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AcademicSettingDao
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.SubjectDao
import com.example.data.local.dao.TimetableDao
import com.example.data.local.entity.AcademicSettingEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TimetableSlotEntity

@Database(
    entities = [
        SubjectEntity::class,
        TimetableSlotEntity::class,
        AttendanceRecordEntity::class,
        AcademicSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun academicSettingDao(): AcademicSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
