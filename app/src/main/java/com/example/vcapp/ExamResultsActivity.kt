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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class ExamResultsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExamResultsScreen()
        }
    }
}

@Composable
fun ExamResultsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    val userEmail = prefs.getString("email", "onbekend") ?: "onbekend"
    
    val latestResult = remember { mutableStateOf<Map<String, Any>?>(null) }
    val allResults = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()) }

    LaunchedEffect(userEmail) {
        val db = FirebaseFirestore.getInstance()
        db.collection("exam_results")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1) // Get latest result
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    latestResult.value = querySnapshot.documents[0].data
                }
                
                // Get all results for history
                db.collection("exam_results")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { allQuerySnapshot ->
                        val results = mutableListOf<Map<String, Any>>()
                        for (doc in allQuerySnapshot) {
                            results.add(doc.data)
                        }
                        allResults.value = results
                        isLoading.value = false
                    }
            }
            .addOnFailureListener {
                isLoading.value = false
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E7FF)),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isLoading.value) {
            Text("Resultaten laden...", fontSize = 18.sp)
            return
        }

        LazyColumn(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Examenresultaten",
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Latest result details
            latestResult.value?.let { result ->
                item {
                    LatestResultCard(result, dateFormat)
                }
            }

            // All results history
            if (allResults.value.isNotEmpty()) {
                item {
                    Text(
                        text = "Eerdere resultaten",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                    )
                }

                items(allResults.value.drop(1)) { result ->
                    ResultHistoryCard(result, dateFormat)
                }
            }

            // Navigation buttons
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            context.startActivity(Intent(context, ExamOverviewActivity::class.java))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3DFE))
                    ) {
                        Text("Nieuw Examen")
                    }
                    
                    Button(
                        onClick = {
                            context.startActivity(Intent(context, DashboardActivity::class.java))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Dashboard")
                    }
                }
            }
        }
    }
}

@Composable
fun LatestResultCard(result: Map<String, Any>, dateFormat: SimpleDateFormat) {
    val context = LocalContext.current
    val score = (result["score"] as? Long)?.toInt() ?: 0
    val totalQuestions = (result["totalQuestions"] as? Long)?.toInt() ?: 0
    val examType = result["examType"] as? String ?: "-"
    val isPassed = result["isPassed"] as? Boolean ?: false
    val passScore = (result["passScore"] as? Long)?.toInt() ?: 0
    val skippedQuestions = (result["skippedQuestions"] as? Long)?.toInt() ?: 0
    val createdAt = result["createdAt"]
    val dateStr = if (createdAt is com.google.firebase.Timestamp) {
        dateFormat.format(createdAt.toDate())
    } else "-"
    val percentage = if (totalQuestions > 0) (score * 100) / totalQuestions else 0
    val questionResults = result["questionResults"] as? List<Map<String, Any>>

    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pass/Fail indicator
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (isPassed) Color(0xFF4CAF50) else Color(0xFFF44336)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPassed) "✓" else "✗",
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isPassed) "GESLAAGD!" else "GEZAKT",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPassed) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Score: $score / $totalQuestions ($percentage%)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Examentype: ${examType.uppercase()}",
                fontSize = 16.sp
            )
            
            Text(
                text = "Datum: $dateStr",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            if (skippedQuestions > 0) {
                Text(
                    text = "Overgeslagen vragen: $skippedQuestions",
                    fontSize = 14.sp,
                    color = Color(0xFFFF9800)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Fun widget - progress circle
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$percentage%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPassed) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Question details button
            if (!questionResults.isNullOrEmpty()) {
                Button(
                    onClick = {
                        // Navigate to detailed question review
                        val intent = Intent(context, DetailedQuestionReviewActivity::class.java).apply {
                            putExtra("questionResults", questionResults.toTypedArray())
                            putExtra("examType", examType)
                            putExtra("score", score)
                            putExtra("totalQuestions", totalQuestions)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3DFE))
                ) {
                    Text("Bekijk alle vragen")
                }
            }

            // Overzicht van alle vragen
            if (!questionResults.isNullOrEmpty()) {
                Text(
                    text = "Vragenoverzicht",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    questionResults.forEachIndexed { idx, q ->
                        val vraag = q["question"] as? String ?: ""
                        val userAnswerIdx = (q["userAnswer"] as? Long)?.toInt() ?: -1
                        val correctAnswerIdx = (q["correctAnswer"] as? Long)?.toInt() ?: -1
                        val isCorrect = q["isCorrect"] as? Boolean ?: false
                        val isSkipped = q["isSkipped"] as? Boolean ?: false
                        val uitleg = q["explanation"] as? String ?: ""
                        val opties = q["options"] as? List<String> ?: emptyList()
                        val onderwerp = q["topic"] as? String ?: ""
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
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
                            Column(modifier = Modifier.padding(28.dp)) {
                                Text(
                                    text = "Vraag ${idx + 1}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = vraag,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                if (onderwerp.isNotBlank()) {
                                    Text(
                                        text = "Onderwerp: $onderwerp",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                if (isSkipped) {
                                    Text(
                                        text = "Overgeslagen",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Jouw antwoord: " + (opties?.getOrNull(userAnswerIdx) ?: "-"),
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 17.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    if (!isCorrect) {
                                        Text(
                                            text = "Juiste antwoord: " + (opties?.getOrNull(correctAnswerIdx) ?: "-"),
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 17.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                                if (uitleg.isNotBlank()) {
                                    Text(
                                        text = "Uitleg: $uitleg",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultHistoryCard(result: Map<String, Any>, dateFormat: SimpleDateFormat) {
    val context = LocalContext.current
    val score = (result["score"] as? Long)?.toInt() ?: 0
    val totalQuestions = (result["totalQuestions"] as? Long)?.toInt() ?: 0
    val examType = result["examType"] as? String ?: "-"
    val isPassed = result["isPassed"] as? Boolean ?: false
    val createdAt = result["createdAt"]
    val dateStr = if (createdAt is com.google.firebase.Timestamp) {
        dateFormat.format(createdAt.toDate())
    } else "-"

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isPassed) Color(0xFF4CAF50) else Color(0xFFF44336)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPassed) "✓" else "✗",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$score / $totalQuestions - ${examType.uppercase()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = dateStr,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Text(
                text = if (isPassed) "GESLAAGD" else "GEZAKT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPassed) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
} 