package com.example.data.repository

import com.example.data.local.dao.SubjectDao
import com.example.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

class SubjectRepository(private val subjectDao: SubjectDao) {

    val activeSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllActiveSubjects()
    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()

    suspend fun getSubjectById(id: Long): SubjectEntity? = subjectDao.getSubjectById(id)

    suspend fun findSubjectByCodeOrName(code: String, name: String): SubjectEntity? {
        if (code.isNotBlank()) {
            val byCode = subjectDao.getSubjectByCode(code.trim())
            if (byCode != null) return byCode
        }
        if (name.isNotBlank()) {
            val byName = subjectDao.getSubjectByName(name.trim())
            if (byName != null) return byName
        }
        return null
    }

    suspend fun insertSubject(subject: SubjectEntity): Long = subjectDao.insertSubject(subject)

    suspend fun insertSubjects(subjects: List<SubjectEntity>): List<Long> = subjectDao.insertSubjects(subjects)

    suspend fun updateSubject(subject: SubjectEntity) = subjectDao.updateSubject(subject)

    suspend fun deleteSubject(subject: SubjectEntity) = subjectDao.deleteSubject(subject)

    suspend fun deleteSubjectById(id: Long) = subjectDao.deleteSubjectById(id)

    suspend fun deleteAllSubjects() = subjectDao.deleteAllSubjects()
}
