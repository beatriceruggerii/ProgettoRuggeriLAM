package com.example.progettoruggerilam.data.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.progettoruggerilam.util.User

@Dao
interface UserDao {

    // Funzione per inserire un nuovo utente nel database
    @Insert
    suspend fun insert(user: User): Long

    // Funzione per ottenere tutti gli utenti nel database
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>

    // Funzione per eliminare un utente in base al nome utente
    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteUserByUsername(username: String)

    // Funzione per contare tutti gli utenti nel database
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    // Funzione per contare gli utenti in base al nome utente (es. per verificare se esiste)
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    suspend fun getUserCountByUsername(username: String): Int

    // Funzione per ottenere un utente in base al nome utente e alla password (login)
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun getUser(username: String, password: String): User?
}
