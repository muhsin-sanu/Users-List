package com.example.machinetest.ui.users

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.machinetest.data.model.User
import com.example.machinetest.databinding.ActivityUserListBinding
import com.example.machinetest.ui.detail.UserDetailActivity
import com.example.machinetest.utils.Constants
import com.example.machinetest.utils.hideKeyboard
import com.example.machinetest.utils.setVisible
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserListBinding
    private val viewModel: UserViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeState()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter { user ->
            navigateToDetail(user)
        }

        val layoutManager = LinearLayoutManager(this)
        binding.rvUsers.layoutManager = layoutManager
        binding.rvUsers.adapter = userAdapter

        // Endless scroll pagination listener
        binding.rvUsers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return // Only paginate when scrolling down

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                val state = viewModel.uiState.value
                if (!state.isLoadingMore && !state.isLastPage && state.searchQuery.isEmpty()) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 4 &&
                        firstVisibleItemPosition >= 0
                    ) {
                        viewModel.loadNextPage()
                    }
                }
            }
        })
    }

    private fun setupListeners() {
        // Swipe to Refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        // Toolbar Refresh Button
        binding.btnToolbarRefresh.setOnClickListener {
            binding.swipeRefreshLayout.isRefreshing = true
            viewModel.refresh()
        }

        // Search Input
        binding.etSearch.doAfterTextChanged { editable ->
            val text = editable?.toString().orEmpty()
            binding.btnClearSearch.setVisible(text.isNotEmpty())
            viewModel.searchUsers(text)
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else false
        }

        // Clear Search
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            hideKeyboard()
        }

        // Retry on Error
        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: UserUiState) {
        // SwipeRefresh loading indicator
        binding.swipeRefreshLayout.isRefreshing = state.isRefreshing

        // Offline notice banner
        binding.bannerOffline.setVisible(state.isOffline)

        // Loading state (initial load only)
        binding.layoutLoading.setVisible(state.isLoading && state.users.isEmpty())

        // Error state (initial failure with no cached/loaded users)
        binding.layoutError.setVisible(state.isInitialError)
        if (state.errorMessage != null) {
            binding.tvErrorMessage.text = state.errorMessage
        }

        // Empty state (no results after query or empty database)
        binding.layoutEmpty.setVisible(state.isEmpty)

        // Show/hide recycler view
        val hasUsers = state.users.isNotEmpty()
        binding.rvUsers.setVisible(hasUsers && !state.isLoading && !state.isInitialError)

        if (hasUsers) {
            userAdapter.submitList(state.users)
        }

        // Toast/Snackbar if pagination failed while users are already visible
        if (state.errorMessage != null && hasUsers && !state.isRefreshing) {
            Snackbar.make(binding.root, state.errorMessage, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun navigateToDetail(user: User) {
        val intent = Intent(this, UserDetailActivity::class.java).apply {
            putExtra(Constants.EXTRA_USER_ID, user.id)
            putExtra(Constants.EXTRA_USER, user)
        }
        startActivity(intent)
    }
}
