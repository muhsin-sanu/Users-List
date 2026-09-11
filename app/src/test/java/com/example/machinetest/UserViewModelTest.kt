package com.example.machinetest

import com.example.machinetest.data.model.User
import com.example.machinetest.data.model.UsersResponse
import com.example.machinetest.data.repository.UserRepository
import com.example.machinetest.ui.users.UserViewModel
import com.example.machinetest.utils.NetworkUtils
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: UserRepository = mockk(relaxed = true)
    private val networkUtils: NetworkUtils = mockk(relaxed = true)

    private val sampleUsers = listOf(
        User(id = 1, firstName = "Emily", lastName = "Johnson", email = "emily@example.com"),
        User(id = 2, firstName = "Michael", lastName = "Williams", email = "michael@example.com")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { networkUtils.observeNetworkStatus } returns flowOf(true)
        coEvery { repository.isOffline() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadInitialUsers updates uiState with users on success`() = runTest {
        val response = UsersResponse(users = sampleUsers, total = 20, skip = 0, limit = 2)
        coEvery { repository.getUsers(limit = any(), skip = 0) } returns Result.success(response)

        val viewModel = UserViewModel(repository, networkUtils)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(2, state.users.size)
        assertEquals("Emily", state.users[0].firstName)
        assertFalse(state.isLastPage)
    }

    @Test
    fun `loadInitialUsers updates uiState with error message on failure`() = runTest {
        coEvery { repository.getUsers(limit = any(), skip = 0) } returns Result.failure(Exception("Network error"))

        val viewModel = UserViewModel(repository, networkUtils)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.errorMessage)
        assertTrue(state.users.isEmpty())
    }

    @Test
    fun `loadNextPage appends new users to existing list`() = runTest {
        val initialResponse = UsersResponse(users = sampleUsers, total = 3, skip = 0, limit = 2)
        coEvery { repository.getUsers(limit = any(), skip = 0) } returns Result.success(initialResponse)

        val nextPageUsers = listOf(
            User(id = 3, firstName = "Sophia", lastName = "Brown", email = "sophia@example.com")
        )
        val nextResponse = UsersResponse(users = nextPageUsers, total = 3, skip = 2, limit = 1)
        coEvery { repository.getUsers(limit = any(), skip = 2) } returns Result.success(nextResponse)

        val viewModel = UserViewModel(repository, networkUtils)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.users.size)

        viewModel.loadNextPage()
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals(3, updatedState.users.size)
        assertTrue(updatedState.isLastPage)
    }
}
