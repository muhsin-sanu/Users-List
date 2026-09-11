package com.example.machinetest.data.remote

import com.example.machinetest.data.model.User
import com.example.machinetest.data.model.UsersResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("users")
    suspend fun getUsers(
        @Query("limit") limit: Int = 10,
        @Query("skip") skip: Int = 0
    ): UsersResponse

    @GET("users/{id}")
    suspend fun getUserById(
        @Path("id") id: Int
    ): User

    @GET("users/search")
    suspend fun searchUsers(
        @Query("q") query: String
    ): UsersResponse
}
