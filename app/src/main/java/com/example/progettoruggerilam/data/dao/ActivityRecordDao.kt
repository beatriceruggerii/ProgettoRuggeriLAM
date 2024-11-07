package com.example.progettoruggerilam.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.progettoruggerilam.data.model.ActivityRecord

@Dao
interface ActivityRecordDao {

    // Inserisce un record nel database e restituisce l'ID del record inserito
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activityRecord: ActivityRecord): Long

    @Query("SELECT * FROM activity_records WHERE userId = :userId AND timestamp BETWEEN :startDate AND :endDate")
    fun getRecordsByUserAndDate(userId: Long, startDate: Long, endDate: Long): LiveData<List<ActivityRecord>>

    // Ottiene tutti i record
    @Query("SELECT * FROM activity_records")
    fun getAllRecords(): LiveData<List<ActivityRecord>>

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUserById(userId: Long)


    // Ottiene i record di un utente
    @Query("SELECT * FROM activity_records WHERE userId = :userId")
    fun getRecordsByUser(userId: Long): LiveData<List<ActivityRecord>>

    // Aggiorna un record di attività esistente
    @Update
    suspend fun update(activityRecord: ActivityRecord)

    // Funzione per cancellare tutti i record di un utente specifico
    @Query("DELETE FROM activity_records WHERE userId = :userId")
    suspend fun deleteRecordsByUserId(userId: Long)

    // Ottiene i record di una data specifica per un utente
    @Query("SELECT * FROM activity_records WHERE DATE(timestamp / 1000, 'unixepoch') = :date AND userId = :userId")
    fun getActivitiesByDateAndUser(date: String, userId: Long): LiveData<List<ActivityRecord>>

    @Query("SELECT * FROM activity_records WHERE userId = :userId AND endTime = 0 LIMIT 1")
    suspend fun getOngoingActivity(userId: Long): ActivityRecord?

}
