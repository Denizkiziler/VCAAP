package com.example.vcapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class DetailedQuestionReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val questionResults = intent.getSerializableExtra("questionResults") as? Array<Map<String, Any>>
        val examType = intent.getStringExtra("examType") ?: ""
        val score = intent.getIntExtra("score", 0)
        val totalQuestions = intent.getIntExtra("totalQuestions", 0)
        
        setContent {
            DetailedQuestionReviewScreen(
                questionResults = questionResults?.toList() ?: emptyList(),
                examType = examType,
                score = score,
                totalQuestions = totalQuestions,
                onBack = {
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedQuestionReviewScreen(
    questionResults: List<Map<String, Any>>,
    examType: String,
    score: Int,
    totalQuestions: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentQuestionIndex by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E7FF))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = "Terug",
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gedetailleerde Review",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = "$examType - $score/$totalQuestions",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            questionResults.forEachIndexed { index, question ->
                val isCorrect = question["isCorrect"] as? Boolean ?: false
                val isSkipped = question["isSkipped"] as? Boolean ?: false
                val isCurrent = index == currentQuestionIndex
                
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> Color(0xFF4B3DFE)
                                isSkipped -> Color(0xFFB8B8B8)
                                isCorrect -> Color(0xFF4CAF50)
                                else -> Color(0xFFF44336)
                            }
                        )
                        .padding(horizontal = 2.dp)
                )
                
                if (index < questionResults.size - 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Current question
        if (questionResults.isNotEmpty()) {
            val currentQuestion = questionResults[currentQuestionIndex]
            QuestionDetailCard(
                question = currentQuestion,
                questionNumber = currentQuestionIndex + 1,
                totalQuestions = questionResults.size
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    if (currentQuestionIndex > 0) {
                        currentQuestionIndex--
                    }
                },
                enabled = currentQuestionIndex > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3DFE))
            ) {
                Text("Vorige")
            }
            
            Text(
                text = "${currentQuestionIndex + 1} / ${questionResults.size}",
                modifier = Modifier.padding(vertical = 16.dp),
                fontWeight = FontWeight.Medium
            )
            
            Button(
                onClick = {
                    if (currentQuestionIndex < questionResults.size - 1) {
                        currentQuestionIndex++
                    }
                },
                enabled = currentQuestionIndex < questionResults.size - 1,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3DFE))
            ) {
                Text("Volgende")
            }
        }
    }
}

@Composable
fun QuestionDetailCard(
    question: Map<String, Any>,
    questionNumber: Int,
    totalQuestions: Int
) {
    val vraag = question["question"] as? String ?: ""
    val userAnswerIdx = (question["userAnswer"] as? Long)?.toInt() ?: -1
    val correctAnswerIdx = (question["correctAnswer"] as? Long)?.toInt() ?: -1
    val isCorrect = question["isCorrect"] as? Boolean ?: false
    val isSkipped = question["isSkipped"] as? Boolean ?: false
    val uitleg = question["explanation"] as? String ?: ""
    val opties = question["options"] as? List<String> ?: emptyList()
    val onderwerp = question["topic"] as? String ?: ""
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSkipped -> Color(0xFFB8B8B8)
                isCorrect -> Color(0xFF4CAF50)
                else -> Color(0xFFF44336)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Question header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vraag $questionNumber van $totalQuestions",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            isSkipped -> "?"
                            isCorrect -> "✓"
                            else -> "✗"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Question text
            Text(
                text = vraag,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Topic
            if (onderwerp.isNotBlank()) {
                Text(
                    text = "Onderwerp: $onderwerp",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Answer options
            if (opties.isNotEmpty()) {
                Text(
                    text = "Antwoordopties:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                opties.forEachIndexed { index, option ->
                    val isUserAnswer = index == userAnswerIdx
                    val isCorrectAnswer = index == correctAnswerIdx
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isCorrectAnswer -> Color(0xFF4CAF50)
                                isUserAnswer && !isCorrect -> Color(0xFFF44336)
                                else -> Color.White.copy(alpha = 0.1f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${('A' + index)}.",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = option,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            if (isUserAnswer) {
                                Text(
                                    text = "Jouw antwoord",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isCorrectAnswer) {
                                Text(
                                    text = "Juist antwoord",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Result summary
            when {
                isSkipped -> {
                    Text(
                        text = "Deze vraag is overgeslagen",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                isCorrect -> {
                    Text(
                        text = "Correct! Je hebt deze vraag goed beantwoord.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                else -> {
                    Text(
                        text = "Fout! Je hebt deze vraag niet correct beantwoord.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            // Explanation
            if (uitleg.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Uitleg:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = uitleg,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
            }
        }
    }
} 