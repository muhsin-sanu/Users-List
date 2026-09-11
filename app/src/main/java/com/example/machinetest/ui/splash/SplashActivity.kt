package com.example.machinetest.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.machinetest.databinding.ActivitySplashBinding
import com.example.machinetest.ui.users.UserListActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animate branding elements
        binding.brandingContainer.alpha = 0f
        binding.brandingContainer.scaleX = 0.85f
        binding.brandingContainer.scaleY = 0.85f
        binding.brandingContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(700)
            .start()

        lifecycleScope.launch {
            delay(1200)
            startActivity(Intent(this@SplashActivity, UserListActivity::class.java))
            finish()
        }
    }
}
