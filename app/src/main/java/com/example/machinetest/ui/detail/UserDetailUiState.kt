package com.example.machinetest.ui.detail

import com.example.machinetest.data.model.User

data class UserDetailUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null
)
