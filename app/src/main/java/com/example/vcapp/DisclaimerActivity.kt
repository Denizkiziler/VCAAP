package com.example.vcapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.clip

class DisclaimerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DisclaimerScreen(onAccept = {
                val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                prefs.edit().putBoolean("disclaimer_accepted", true).apply()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            })
        }
    }
}

@Composable
fun DisclaimerScreen(onAccept: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E7FF)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Disclaimer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Deze app is bedoeld als hulpmiddel om je voor te bereiden op het VCA-examen. De inhoud van deze app is met zorg samengesteld, maar kan onjuistheden of onvolledigheden bevatten. De app is experimenteel en wordt continu verbeterd. Er kunnen geen rechten worden ontleend aan de resultaten, oefenvragen of andere informatie in deze app.\n\nHet gebruik van deze app is volledig op eigen risico. De makers van deze app zijn niet aansprakelijk voor eventuele schade, verlies van gegevens, of andere gevolgen die voortvloeien uit het gebruik van deze app. Voor officiële en actuele informatie over het VCA-examen en de bijbehorende regelgeving, raadpleeg altijd de erkende instanties.\n\nDoor deze app te gebruiken ga je akkoord met deze voorwaarden.",
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Akkoord", fontSize = 18.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DisclaimerScreenPreview() {
    DisclaimerScreen(onAccept = {})
} 