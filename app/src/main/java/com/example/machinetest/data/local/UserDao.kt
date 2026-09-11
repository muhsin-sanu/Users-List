package com.example.machinetest.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id ASC LIMIT :limit OFFSET :offset")
    suspend fun getUsers(limit: Int, offset: Int): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Int): UserEntity?

    @Query("SELECT * FROM users WHERE firstName LIKE '%' || :query || '%' OR lastName LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR role LIKE '%' || :query || '%'")
    suspend fun searchUsers(query: String): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}
