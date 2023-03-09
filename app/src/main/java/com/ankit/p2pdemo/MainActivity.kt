package com.ankit.p2pdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View.GONE
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ankit.p2pdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fl_container, PhoneFragment())
                binding.button.visibility = GONE
                binding.button2.visibility = GONE
            }
        }

        binding.button2.setOnClickListener {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fl_container, GlassesFragment())
                binding.button.visibility = GONE
                binding.button2.visibility = GONE
            }
        }
    }
}