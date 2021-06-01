package com.netDashboard.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.netDashboard.activities.dashboard.DashboardActivity
import com.netDashboard.databinding.MainActivityBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: MainActivityBinding
    private val counter = MutableLiveData(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = MainActivityBinding.inflate(layoutInflater)
        setContentView(b.root)

        Handler(Looper.getMainLooper()).postDelayed({
            Intent(this, DashboardActivity::class.java).also {
                it.putExtra("dashboardName", "main")

                startActivity(it)
                overridePendingTransition(0, 0)

                finish()
            }
        }, 0) //3300
    }
}