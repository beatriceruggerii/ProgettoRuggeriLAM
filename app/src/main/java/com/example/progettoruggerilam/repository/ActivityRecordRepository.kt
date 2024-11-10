package com.example.progettoruggerilam.repository

import androidx.lifecycle.LiveData
import com.example.progettoruggerilam.data.dao.ActivityRecordDao
import com.example.progettoruggerilam.data.model.ActivityRecord


class ActivityRecordRepository(private val activityRecordDao: ActivityRecordDao) {

    // Modifica la funzione insert per restituire un Long
    suspend fun insert(activityRecord: ActivityRecord): Long {
        return activityRecordDao.insert(activityRecord)
    }

    suspend fun deleteRecordsByUserId(userId: Long) {
        activityRecordDao.deleteRecordsByUserId(userId)
    }

    fun getRecordsByUserAndDate(userId: Long, startDate: Long, endDate: Long): LiveData<List<ActivityRecord>> {
        return activityRecordDao.getRecordsByUserAndDate(userId, startDate, endDate)
    }

    suspend fun deleteUserAccountById(userId: Long) {
        activityRecordDao.deleteUserById(userId)
    }

    fun getActivitiesByDateAndUser(date: String, userId: Long): LiveData<List<ActivityRecord>> {
        return activityRecordDao.getActivitiesByDateAndUser(date, userId)
    }


}
