package com.example.machinetest.ui.users

import com.example.machinetest.data.model.User

data class UserUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val users: List<User> = emptyList(),
    val errorMessage: String? = null,
    val isLastPage: Boolean = false,
    val isOffline: Boolean = false,
    val searchQuery: String = ""
) {
    val isEmpty: Boolean
        get() = !isLoading && !isRefreshing && errorMessage == null && users.isEmpty()

    val isInitialError: Boolean
        get() = !isLoading && !isRefreshing && errorMessage != null && users.isEmpty()
}
