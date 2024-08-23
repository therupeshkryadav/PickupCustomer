package com.bussiness.pickup_customer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.bussiness.pickup_customer.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private val SPLASH_DISPLAY_LENGTH = 2000L // Splash screen delay time in milliseconds
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using the binding class
        binding = ActivitySplashBinding.inflate(layoutInflater)

        // Remove the title bar
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)



        Handler().postDelayed({
            // Start the main activity
            val intent = Intent(this@SplashActivity, ChoiceActivity::class.java)
            startActivity(intent)
            finish()
        }, SPLASH_DISPLAY_LENGTH)
    }
}