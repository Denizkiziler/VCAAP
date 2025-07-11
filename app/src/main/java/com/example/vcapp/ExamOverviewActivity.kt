package com.example.vcapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.vcapp.ui.StatItem

class ExamOverviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExamOverviewScreen()
        }
    }
}

data class ExamInfo(
    val id: String,
    val title: String,
    val description: String,
    val questionCount: Int,
    val timeLimit: Int,
    val passingScore: Int,
    val isCompleted: Boolean = false,
    val bestScore: Int? = null
)

@Composable
fun ExamOverviewScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    val userEmail = prefs.getString("email", "onbekend") ?: "onbekend"
    val userLanguage = prefs.getString("lang", java.util.Locale.getDefault().language) ?: "nl"
    
    val completedExams = remember { mutableStateOf(0) }
    val averageScore = remember { mutableStateOf(0) }
    val bestScore = remember { mutableStateOf(0) }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }

    val showResumeDialog = remember { mutableStateOf(false) }
    val pausedExamId = remember { mutableStateOf<String?>(null) }
    val pausedExamTitle = remember { mutableStateOf<String?>(null) }
    val pausedCurrentQuestion = remember { mutableStateOf(0) }
    val pausedSelectedAnswers = remember { mutableStateOf<List<Int>>(emptyList()) }
    val pausedSkippedQuestions = remember { mutableStateOf<Set<Int>>(emptySet()) }
    val pausedTimeLeft = remember { mutableStateOf(0) }

    // Load exams from Firestore
    val exams = remember { mutableStateOf<List<ExamInfo>>(emptyList()) }
    val isExamsLoading = remember { mutableStateOf(true) }

    // Color palette for exam cards
    val cardColors = listOf(
        Color(0xFF14B8A6), // Teal
        Color(0xFF4F46E5), // Indigo
        Color(0xFFF97316), // Orange
        Color(0xFFA78BFA), // Purple
        Color(0xFF0E9488), // Green
        Color(0xFF6366F1), // Blue
        Color(0xFFF59E42)  // Light Orange
    )

    // Helper function to get localized content from translations
    fun getLocalizedContent(data: Map<String, Any?>, field: String, userLanguage: String): String {
        return try {
            val translations = data["translations"] as? Map<*, *>
            if (translations != null) {
                val langData = translations[userLanguage] as? Map<*, *>
                if (langData != null) {
                    return langData[field] as? String ?: ""
                }
                // Fallback to Dutch
                val nlData = translations["nl"] as? Map<*, *>
                if (nlData != null) {
                    return nlData[field] as? String ?: ""
                }
            }
            // Fallback to main field
            data[field] as? String ?: ""
        } catch (e: Exception) {
            android.util.Log.e("ExamOverviewActivity", "Error getting localized content for field $field", e)
            data[field] as? String ?: ""
        }
    }

    // --- LAATSTE 3 EXAMENS LOKAAL ---
    val gson = remember { Gson() }
    val localResults = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    LaunchedEffect(Unit) {
        val json = prefs.getString("local_exam_results", null)
        if (json != null) {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            localResults.value = gson.fromJson(json, type) ?: emptyList()
        }
    }

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

    // Load exams from Firestore
    LaunchedEffect(userLanguage) {
        val db = FirebaseFirestore.getInstance()
        db.collection("examenVragen")
            .get()
            .addOnSuccessListener { querySnapshot ->
                try {
                    android.util.Log.d("ExamOverviewActivity", "Found ${querySnapshot.documents.size} exam questions")
                    
                    val examList = mutableListOf<ExamInfo>()
                    
                    // Count questions by exam type
                    val questionCounts = mutableMapOf<String, Int>()
                    val examTypes = mutableSetOf<String>()
                    
                    querySnapshot.documents.forEach { doc ->
                        val data = doc.data
                        if (data != null) {
                            val examTypesList = data["examTypes"] as? List<String> ?: listOf("vca-basis")
                            examTypes.addAll(examTypesList)
                            
                            examTypesList.forEach { examType ->
                                val normalizedType = when (examType) {
                                    "vca-basis" -> "basis"
                                    "vca-vol" -> "vol"
                                    else -> examType
                                }
                                questionCounts[normalizedType] = (questionCounts[normalizedType] ?: 0) + 1
                            }
                        }
                    }
                    
                    // Create exam info for each exam type
                    examTypes.forEach { examType ->
                        val normalizedType = when (examType) {
                            "vca-basis" -> "basis"
                            "vca-vol" -> "vol"
                            else -> examType
                        }
                        
                        val questionCount = questionCounts[normalizedType] ?: 0
                        val actualQuestionCount = when (normalizedType) {
                            "vol" -> minOf(70, questionCount)
                            "basis" -> minOf(40, questionCount)
                            else -> minOf(40, questionCount)
                        }
                        
                        val timeLimit = when (normalizedType) {
                            "vol" -> 105
                            "basis" -> 60
                            else -> 60
                        }
                        
                        val passingScore = when (normalizedType) {
                            "vol" -> 70
                            "basis" -> 70
                            else -> 70
                        }
                        
                        examList.add(
                            ExamInfo(
                                id = normalizedType,
                                title = when (normalizedType) {
                                    "vol" -> "VCA VOL Examen"
                                    "basis" -> "VCA Basis Examen"
                                    else -> "VCA Examen"
                                },
                                description = when (normalizedType) {
                                    "vol" -> "VCA voor Operationeel Leidinggevenden"
                                    "basis" -> "Basis VCA certificering voor alle medewerkers"
                                    else -> "VCA certificering"
                                },
                                questionCount = actualQuestionCount,
                                timeLimit = timeLimit,
                                passingScore = passingScore,
                                isCompleted = completedExams.value > 0,
                                bestScore = if (bestScore.value > 0) bestScore.value else null
                            )
                        )
                    }
                    
                    // If no exams found, create default exams
                    if (examList.isEmpty()) {
                        examList.add(
                            ExamInfo(
                                id = "basis",
                                title = "VCA Basis Examen",
                                description = "Basis VCA certificering voor alle medewerkers",
                                questionCount = 40,
                                timeLimit = 60,
                                passingScore = 70,
                                isCompleted = completedExams.value > 0,
                                bestScore = if (bestScore.value > 0) bestScore.value else null
                            )
                        )
                        examList.add(
                            ExamInfo(
                                id = "vol",
                                title = "VCA VOL Examen",
                                description = "VCA voor Operationeel Leidinggevenden",
                                questionCount = 70,
                                timeLimit = 105,
                                passingScore = 70,
                                isCompleted = false,
                                bestScore = null
                            )
                        )
                    }
                    
                    exams.value = examList
                    isExamsLoading.value = false
                } catch (e: Exception) {
                    android.util.Log.e("ExamOverviewActivity", "Error processing exam data", e)
                    hasError.value = true
                    isExamsLoading.value = false
                }
            }
            .addOnFailureListener { 
                android.util.Log.e("ExamOverviewActivity", "Failed to load exams", it)
                hasError.value = true
                isExamsLoading.value = false 
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

    // Main background with gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE0E7FF),
                        Color(0xFFC7D2FE)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF14B8A6)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF14B8A6),
                                    Color(0xFF0E9488)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "VCA Examens",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "Test je kennis en behaal je certificering",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Stats row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            StatItem("Voltooid", "${completedExams.value}", Color.White)
                            StatItem("Gemiddeld", "${averageScore.value}%", Color.White)
                            StatItem("Beste", "${bestScore.value}%", Color.White)
                        }
                    }
                }
            }
            
            // Content area
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // --- RESULTATEN KOP EN OVERZICHT ---
                    Text(
                        "Laatste 3 resultaten",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (localResults.value.isEmpty()) {
                        Text("Nog geen resultaten", color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 16.dp))
                    } else {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            localResults.value.take(3).forEach { result ->
                                val score = result["score"] as? Double ?: (result["score"] as? Long)?.toDouble() ?: 0.0
                                val total = result["totalQuestions"] as? Double ?: (result["totalQuestions"] as? Long)?.toDouble() ?: 1.0
                                val date = result["createdAt"]?.toString()?.substring(0, 16) ?: "-"
                                val passed = (result["isPassed"] as? Boolean) ?: (score / total >= 0.7)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (passed) Color(0xFF4CAF50) else Color(0xFFF44336))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (passed) "Geslaagd" else "Gezakt",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(80.dp)
                                        )
                                        Text(
                                            "Score: ${score.toInt()} / ${total.toInt()} (${((score/total)*100).toInt()}%)",
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            date,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isExamsLoading.value) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (hasError.value) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Error loading exams", color = MaterialTheme.colorScheme.error)
                                Button(onClick = {
                                    isExamsLoading.value = true
                                    hasError.value = false
                                }) {
                                    Text("Retry")
                                }
                            }
                        }
                    } else if (exams.value.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Geen examens gevonden")
                        }
                    } else {
                        // Colorful cards for exams
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            exams.value.forEachIndexed { idx, exam ->
                                val color = cardColors[idx % cardColors.size]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val intent = Intent(context, ExamIntroActivity::class.java)
                                            intent.putExtra("exam_id", exam.id)
                                            intent.putExtra("exam_title", exam.title)
                                            intent.putExtra("max_questions", exam.questionCount)
                                            intent.putExtra("time_limit_minutes", exam.timeLimit)
                                            context.startActivity(intent)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = color)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp, horizontal = 16.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            exam.title,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            exam.description,
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Text(
                                                "Vragen: ${exam.questionCount}",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                "Tijd: ${exam.timeLimit} min",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                "Slagingsdrempel: ${exam.passingScore}%",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                        
                                        // Show best score if available
                                        exam.bestScore?.let { score ->
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Beste score: $score%",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.9f)
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