package com.netDashboard.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.netDashboard.databinding.ActivitySplashScreenBinding
import java.util.*
import kotlin.concurrent.schedule

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var b: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(b.root)

        val con = this
        Timer().schedule(500) {
            Intent(con, MainActivity::class.java).also {
                finish()
                startActivity(it)
            }
        }
    }
}
