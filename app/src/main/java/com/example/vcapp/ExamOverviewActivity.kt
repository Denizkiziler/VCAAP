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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.vcapp.data.MockContentData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ExamOverviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExamOverviewScreen()
        }
    }
}

@Composable
fun ExamOverviewScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    val userEmail = prefs.getString("email", "onbekend") ?: "onbekend"
    
    val completedExams = remember { mutableStateOf(0) }
    val averageScore = remember { mutableStateOf(0) }
    val bestScore = remember { mutableStateOf(0) }
    val isLoading = remember { mutableStateOf(true) }

    val showResumeDialog = remember { mutableStateOf(false) }
    val pausedExamId = remember { mutableStateOf<String?>(null) }
    val pausedExamTitle = remember { mutableStateOf<String?>(null) }
    val pausedCurrentQuestion = remember { mutableStateOf(0) }
    val pausedSelectedAnswers = remember { mutableStateOf<List<Int>>(emptyList()) }
    val pausedSkippedQuestions = remember { mutableStateOf<Set<Int>>(emptySet()) }
    val pausedTimeLeft = remember { mutableStateOf(0) }

    // Load statistics from Firebase
    LaunchedEffect(userEmail) {
        val db = FirebaseFirestore.getInstance()
        db.collection("exam_results")
            .whereEqualTo("userId", userEmail)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val results = mutableListOf<Map<String, Any>>()
                for (doc in querySnapshot) {
                    results.add(doc.data)
                }
                
                completedExams.value = results.size
                
                if (results.isNotEmpty()) {
                    val totalScore = results.sumOf { (it["score"] as? Long)?.toInt() ?: 0 }
                    val totalQuestions = results.sumOf { (it["totalQuestions"] as? Long)?.toInt() ?: 0 }
                    
                    if (totalQuestions > 0) {
                        averageScore.value = (totalScore * 100) / totalQuestions
                    }
                    
                    val scores = results.mapNotNull { result ->
                        val score = (result["score"] as? Long)?.toInt() ?: 0
                        val total = (result["totalQuestions"] as? Long)?.toInt() ?: 0
                        if (total > 0) (score * 100) / total else null
                    }
                    
                    if (scores.isNotEmpty()) {
                        bestScore.value = scores.maxOrNull() ?: 0
                    }
                }
                
                isLoading.value = false
            }
            .addOnFailureListener {
                isLoading.value = false
            }
    }

    // Resultatenoverzicht van laatste 3 examens
    val recentResults = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val isResultsLoading = remember { mutableStateOf(true) }
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()) }
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("exam_results")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val results = mutableListOf<Map<String, Any>>()
                for (doc in querySnapshot) {
                    results.add(doc.data)
                }
                recentResults.value = results
                isResultsLoading.value = false
            }
            .addOnFailureListener {
                isResultsLoading.value = false
            }
    }

    // Check op gepauzeerd examen bij openen
    LaunchedEffect(Unit) {
        val id = prefs.getString("paused_exam_id", null)
        val title = prefs.getString("paused_exam_title", null)
        if (id != null && title != null) {
            showResumeDialog.value = true
            pausedExamId.value = id
            pausedExamTitle.value = title
            pausedCurrentQuestion.value = prefs.getInt("paused_current_question", 0)
            pausedSelectedAnswers.value = prefs.getString("paused_selected_answers", "")?.split(",")?.map { it.toIntOrNull() ?: -1 } ?: emptyList()
            pausedSkippedQuestions.value = prefs.getString("paused_skipped_questions", "")?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
            pausedTimeLeft.value = prefs.getInt("paused_time_left", 0)
        }
    }

    val exams = listOf(
        ExamInfo(
            id = "1",
            title = "VCA Basis Examen",
            description = "Basis VCA certificering voor alle medewerkers",
            questionCount = 40,
            timeLimit = 60,
            passingScore = 70,
            isCompleted = completedExams.value > 0,
            bestScore = if (bestScore.value > 0) bestScore.value else null
        ),
        ExamInfo(
            id = "2",
            title = "VCA VOL Examen",
            description = "VCA voor Operationeel Leidinggevenden",
            questionCount = 70,
            timeLimit = 105,
            passingScore = 70,
            isCompleted = false,
            bestScore = null
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "VCA Examens",
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Test je kennis en behaal je VCA certificering",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Statistics card
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4B3DFE))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jouw Statistieken",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    Button(
                        onClick = {
                            context.startActivity(Intent(context, ExamResultsActivity::class.java))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.size(height = 32.dp, width = 100.dp)
                    ) {
                        Text(
                            "Resultaten",
                            color = Color(0xFF4B3DFE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        "Voltooide examens", 
                        if (isLoading.value) "..." else "${completedExams.value}", 
                        Color.White
                    )
                    StatItem(
                        "Gemiddelde score", 
                        if (isLoading.value) "..." else "${averageScore.value}%", 
                        Color.White
                    )
                    StatItem(
                        "Best score", 
                        if (isLoading.value) "..." else "${bestScore.value}%", 
                        Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Resultatenoverzicht tonen boven beschikbare examens
        if (isResultsLoading.value) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFF4B3DFE))
            }
        } else if (recentResults.value.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Laatste resultaten",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF4B3DFE),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                recentResults.value.forEach { result ->
                    val score = (result["score"] as? Long)?.toInt() ?: 0
                    val totalQuestions = (result["totalQuestions"] as? Long)?.toInt() ?: 0
                    val isPassed = result["isPassed"] as? Boolean ?: false
                    val createdAt = result["createdAt"]
                    val dateStr = if (createdAt is com.google.firebase.Timestamp) {
                        dateFormat.format(createdAt.toDate())
                    } else "-"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isPassed) Color(0xFF4CAF50) else Color(0xFFF44336)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPassed) "✓" else "✗",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$score/$totalQuestions", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(dateStr, color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Available exams
        Text(
            text = "Beschikbare Examens",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LocalResultsOverview()
        
        ExamList(exams)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Dashboard button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = {
                    context.startActivity(Intent(context, DashboardActivity::class.java))
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3DFE))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Dashboard",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (showResumeDialog.value && pausedExamId.value != null) {
            AlertDialog(
                onDismissRequest = { showResumeDialog.value = false },
                title = { Text("Gepauzeerd examen hervatten?") },
                text = { Text("Je hebt een onafgemaakt examen: ${pausedExamTitle.value}. Wil je verdergaan of een nieuw examen starten?") },
                confirmButton = {
                    TextButton(onClick = {
                        showResumeDialog.value = false
                        // Start ExamQuestionActivity met voortgang
                        val intent = Intent(context, ExamQuestionActivity::class.java)
                        intent.putExtra("exam_id", pausedExamId.value)
                        intent.putExtra("exam_title", pausedExamTitle.value)
                        intent.putExtra("resume", true)
                        context.startActivity(intent)
                    }) {
                        Text("Verdergaan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        // Verwijder pauze-data
                        val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                        prefs.edit().remove("paused_exam_id")
                            .remove("paused_exam_title")
                            .remove("paused_current_question")
                            .remove("paused_selected_answers")
                            .remove("paused_skipped_questions")
                            .remove("paused_time_left")
                            .apply()
                        showResumeDialog.value = false
                    }) {
                        Text("Nieuw examen")
                    }
                }
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, textColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = label,
            color = textColor.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun ExamList(exams: List<ExamInfo>) {
    val context = LocalContext.current
    val showDialog = remember { mutableStateOf(false) }
    val selectedExam = remember { mutableStateOf<ExamInfo?>(null) }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        exams.forEach { exam ->
            ExamCard(
                exam = exam,
                onClick = {
                    selectedExam.value = exam
                    showDialog.value = true
                }
            )
        }
    }
    if (showDialog.value && selectedExam.value != null) {
        val exam = selectedExam.value!!
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = {
                Text(text = exam.title, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            },
            text = {
                Column {
                    Text("Aantal vragen: ${exam.questionCount}", fontSize = 18.sp)
                    Text("Tijdslimiet: ${exam.timeLimit} minuten", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("${exam.description}", fontSize = 16.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Lees de vraag goed en kies het juiste antwoord. Succes!", fontSize = 16.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog.value = false
                    val intent = Intent(context, ExamQuestionActivity::class.java)
                    intent.putExtra("exam_id", exam.id)
                    intent.putExtra("exam_title", exam.title)
                    intent.putExtra("max_questions", exam.questionCount)
                    intent.putExtra("time_limit_minutes", exam.timeLimit)
                    context.startActivity(intent)
                }) {
                    Text("Start Examen", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text("Annuleren")
                }
            }
        )
    }
}

@Composable
fun ExamCard(
    exam: ExamInfo,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_examen),
                    contentDescription = exam.title,
                    tint = Color(0xFF4B3DFE),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = exam.description,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                if (exam.isCompleted) {
                    Text(
                        text = "✓",
                        color = Color.Green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ExamDetail("Vragen", "${exam.questionCount}")
                ExamDetail("Tijd", "${exam.timeLimit} min")
                ExamDetail("Slagingspercentage", "${exam.passingScore}%")
                exam.bestScore?.let { score ->
                    ExamDetail("Beste score", "${score}%")
                }
            }
        }
    }
}

@Composable
fun ExamDetail(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

data class ExamInfo(
    val id: String,
    val title: String,
    val description: String,
    val questionCount: Int,
    val timeLimit: Int,
    val passingScore: Int,
    val isCompleted: Boolean,
    val bestScore: Int?
)

@Composable
fun LocalResultsOverview() {
    val context = LocalContext.current
    val results = remember { mutableStateOf(getLocalExamResults(context)) }

    if (results.value.isNotEmpty()) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Laatste 3 lokale resultaten", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            results.value.forEachIndexed { idx, result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Examen: ${result["examType"]}", fontWeight = FontWeight.Bold)
                        Text("Score: ${result["score"]} / ${result["totalQuestions"]}")
                        val date = (result["createdAt"] as? Double)?.toLong() ?: (result["createdAt"] as? Long) ?: 0L
                        if (date > 0) {
                            val dateStr = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(date))
                            Text("Datum: $dateStr")
                        }
                    }
                }
            }
        }
    }
}

fun getLocalExamResults(context: android.content.Context): List<Map<String, Any>> {
    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    val gson = Gson()
    val json = prefs.getString("local_exam_results", null)
    return if (json != null) {
        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    } else {
        emptyList()
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ExamOverviewScreenPreview() {
    ExamOverviewScreen()
} 