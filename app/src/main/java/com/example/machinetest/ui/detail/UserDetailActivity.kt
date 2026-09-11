package com.example.machinetest.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.transform.CircleCropTransformation
import com.example.machinetest.R
import com.example.machinetest.data.model.User
import com.example.machinetest.databinding.ActivityUserDetailBinding
import com.example.machinetest.utils.Constants
import com.example.machinetest.utils.setVisible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDetailBinding
    private val viewModel: UserDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
        parseIntentData()
        observeState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        binding.btnDetailRetry.setOnClickListener {
            viewModel.retry()
        }
    }

    @Suppress("DEPRECATION")
    private fun parseIntentData() {
        val user: User? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(Constants.EXTRA_USER, User::class.java)
        } else {
            intent.getSerializableExtra(Constants.EXTRA_USER) as? User
        }

        val userId = intent.getIntExtra(Constants.EXTRA_USER_ID, -1)

        if (user != null) {
            viewModel.setUser(user)
        } else if (userId != -1) {
            viewModel.loadUser(userId)
        } else {
            Toast.makeText(this, "No user information provided", Toast.LENGTH_SHORT).show()
            finish()
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

    private fun renderState(state: UserDetailUiState) {
        // Loading
        binding.progressBarDetail.setVisible(state.isLoading && state.user == null)

        // Error
        binding.layoutDetailError.setVisible(state.errorMessage != null && state.user == null)
        binding.tvDetailErrorMessage.text = state.errorMessage.orEmpty()

        // Content
        val hasUser = state.user != null
        binding.scrollContent.setVisible(hasUser)

        if (state.user != null) {
            bindUserDetails(state.user)
        }
    }

    private fun bindUserDetails(user: User) {
        binding.tvDetailFullName.text = user.fullName
        binding.tvDetailUsername.text = if (user.username.isNotBlank()) "@${user.username}" else ""
        binding.tvDetailRoleBadge.text = user.formattedRole

        // Avatar
        binding.ivDetailAvatar.load(user.image) {
            crossfade(true)
            placeholder(R.drawable.bg_avatar_placeholder)
            error(R.drawable.ic_person)
            transformations(CircleCropTransformation())
        }

        // Action Buttons
        binding.btnCall.setOnClickListener {
            if (user.phone.isNotBlank()) {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone}"))
                startActivity(dialIntent)
            } else {
                Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEmail.setOnClickListener {
            if (user.email.isNotBlank()) {
                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${user.email}"))
                startActivity(Intent.createChooser(emailIntent, "Send Email"))
            } else {
                Toast.makeText(this, "No email address available", Toast.LENGTH_SHORT).show()
            }
        }

        // Contact Info
        binding.tvDetailEmail.text = user.email.ifBlank { "N/A" }
        binding.tvDetailPhone.text = user.phone.ifBlank { "N/A" }
        binding.tvDetailAge.text = if (user.age > 0) user.age.toString() else "N/A"
        binding.tvDetailGender.text = user.gender.replaceFirstChar { it.uppercase() }.ifBlank { "N/A" }
        binding.tvDetailBloodGroup.text = user.bloodGroup ?: "N/A"

        // Company / Work
        binding.tvDetailCompanyName.text = user.company?.name ?: "N/A"
        binding.tvDetailJobTitle.text = user.company?.title ?: "N/A"
        binding.tvDetailDepartment.text = user.company?.department ?: "N/A"

        // Address
        binding.tvDetailAddress.text = user.address?.address ?: "N/A"
        val cityState = listOfNotNull(user.address?.city, user.address?.state).joinToString(", ")
        binding.tvDetailCityState.text = cityState.ifBlank { "N/A" }
        val countryZip = listOfNotNull(user.address?.country, user.address?.postalCode?.let { "($it)" }).joinToString(" ")
        binding.tvDetailCountry.text = countryZip.ifBlank { "N/A" }
    }
}
