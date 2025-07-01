package com.example.vcapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashScreen()
                }
            }
        }
        
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val disclaimerAccepted = prefs.getBoolean("disclaimer_accepted", false)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (disclaimerAccepted) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    } else {
                        startActivity(Intent(this, DisclaimerActivity::class.java))
                    }
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback to main activity if there's an error
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }, 1500)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to main activity if there's an error
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }, 1500)
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "VCA-APP logo",
            modifier = Modifier.size(180.dp)
        )
    }
}

@Preview
@Composable
fun SplashScreenPreview() {
    SplashScreen()
} 