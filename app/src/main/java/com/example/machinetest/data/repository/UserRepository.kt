package com.example.machinetest.data.repository

import com.example.machinetest.data.local.UserDao
import com.example.machinetest.data.local.UserEntity
import com.example.machinetest.data.model.User
import com.example.machinetest.data.model.UsersResponse
import com.example.machinetest.data.remote.ApiService
import com.example.machinetest.utils.NetworkUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    suspend fun getUsers(limit: Int, skip: Int): Result<UsersResponse>
    suspend fun getUserById(userId: Int): Result<User>
    suspend fun searchUsers(query: String): Result<UsersResponse>
    suspend fun isOffline(): Boolean
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val networkUtils: NetworkUtils,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserRepository {

    override suspend fun isOffline(): Boolean = withContext(ioDispatcher) {
        !networkUtils.isNetworkAvailable()
    }

    override suspend fun getUsers(limit: Int, skip: Int): Result<UsersResponse> = withContext(ioDispatcher) {
        if (!networkUtils.isNetworkAvailable()) {
            return@withContext fetchFromLocal(limit, skip, isOffline = true)
        }

        try {
            val response = apiService.getUsers(limit = limit, skip = skip)
            // Cache in Room
            if (response.users.isNotEmpty()) {
                userDao.insertUsers(response.users.map { UserEntity.fromUser(it) })
            }
            Result.success(response)
        } catch (e: Exception) {
            // Attempt fallback to local cache
            fetchFromLocal(limit, skip, isOffline = false, fallbackError = e)
        }
    }

    private suspend fun fetchFromLocal(
        limit: Int,
        skip: Int,
        isOffline: Boolean,
        fallbackError: Exception? = null
    ): Result<UsersResponse> {
        val cached = userDao.getUsers(limit = limit, offset = skip)
        val totalCached = userDao.getUserCount()
        return if (cached.isNotEmpty()) {
            Result.success(
                UsersResponse(
                    users = cached.map { it.toUser() },
                    total = totalCached,
                    skip = skip,
                    limit = limit
                )
            )
        } else {
            val errorMsg = if (isOffline) {
                "No internet connection and no cached data available."
            } else {
                fallbackError?.localizedMessage ?: "Failed to load users."
            }
            Result.failure(Exception(errorMsg, fallbackError))
        }
    }

    override suspend fun getUserById(userId: Int): Result<User> = withContext(ioDispatcher) {
        if (networkUtils.isNetworkAvailable()) {
            try {
                val user = apiService.getUserById(userId)
                userDao.insertUsers(listOf(UserEntity.fromUser(user)))
                return@withContext Result.success(user)
            } catch (_: Exception) {
                // fallback to local
            }
        }

        val cachedUser = userDao.getUserById(userId)?.toUser()
        if (cachedUser != null) {
            Result.success(cachedUser)
        } else {
            Result.failure(Exception("Unable to load user details. Please check your internet connection."))
        }
    }

    override suspend fun searchUsers(query: String): Result<UsersResponse> = withContext(ioDispatcher) {
        if (networkUtils.isNetworkAvailable()) {
            try {
                val response = apiService.searchUsers(query)
                return@withContext Result.success(response)
            } catch (_: Exception) {
                // fallback to local search
            }
        }

        val localMatches = userDao.searchUsers(query)
        Result.success(
            UsersResponse(
                users = localMatches.map { it.toUser() },
                total = localMatches.size,
                skip = 0,
                limit = localMatches.size
            )
        )
    }
}
