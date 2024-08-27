package com.bussiness.pickup_customer

import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.bussiness.pickup_customer.customerStack.CustomerLoginActivity
import com.bussiness.pickup_customer.databinding.ActivityChoiceBinding

class ChoiceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChoiceBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using the binding class
        binding = ActivityChoiceBinding.inflate(layoutInflater)
        // Remove the title bar
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        binding.apply {
            iMCustomer.setOnClickListener {
                startActivity(Intent(this@ChoiceActivity, CustomerLoginActivity::class.java))
                finish()
            }
        }
    }
}