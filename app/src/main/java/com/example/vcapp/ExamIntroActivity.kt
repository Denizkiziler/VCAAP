package com.example.vcapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ExamIntroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val examId = intent.getStringExtra("exam_id") ?: "1"
        val examTitle = intent.getStringExtra("exam_title") ?: "VCA Examen"
        val maxQuestions = intent.getIntExtra("max_questions", 40)
        val timeLimitMinutes = intent.getIntExtra("time_limit_minutes", 60)
        setContent {
            ExamIntroScreen(
                examId = examId,
                examTitle = examTitle,
                maxQuestions = maxQuestions,
                timeLimitMinutes = timeLimitMinutes
            )
        }
    }
}

@Composable
fun ExamIntroScreen(
    examId: String,
    examTitle: String,
    maxQuestions: Int,
    timeLimitMinutes: Int
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        Color.White
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = examTitle,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Welkom bij het oefenexamen! Je krijgt $maxQuestions vragen. Je hebt $timeLimitMinutes minuten de tijd. Lees de vraag goed en kies het juiste antwoord. Succes!",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Vragen", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("$maxQuestions", fontSize = 20.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tijd", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("$timeLimitMinutes min", fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        val intent = Intent(context, ExamQuestionActivity::class.java)
                        intent.putExtra("exam_id", examId)
                        intent.putExtra("exam_title", examTitle)
                        intent.putExtra("max_questions", maxQuestions)
                        intent.putExtra("time_limit_minutes", timeLimitMinutes)
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Examen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
} 