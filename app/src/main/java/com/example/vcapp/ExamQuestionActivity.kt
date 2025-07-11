package com.example.vcapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.util.Date
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.gson.Gson
import com.example.vcapp.TTSHelper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import android.util.Log
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll

class ExamQuestionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExamQuestionScreen()
        }
    }
}

data class ExamQuestion(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: Int = 0,
    val explanation: String = "",
    val imageUrl: String? = null
)

@Composable
fun ExamQuestionScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    val userEmail = prefs.getString("email", "onbekend") ?: "onbekend"
    val userLanguage = prefs.getString("lang", java.util.Locale.getDefault().language) ?: "nl"
    val activity = context as? ComponentActivity
    val examId = remember { mutableStateOf(activity?.intent?.getStringExtra("exam_id") ?: "basis") }
    val examTitle = remember { mutableStateOf(activity?.intent?.getStringExtra("exam_title") ?: "VCA Basis Examen") }
    val isVolExam = remember { mutableStateOf(examId.value == "vol") }
    val maxQuestions = remember { mutableStateOf(if (isVolExam.value) 70 else 40) }
    val timeLimitMinutes = remember { mutableStateOf(if (isVolExam.value) 105 else 60) }
    val timeLimitSeconds = remember { mutableStateOf(timeLimitMinutes.value * 60) }

    val questions = remember { mutableStateOf<List<ExamQuestion>>(emptyList()) }
    val selectedAnswers = remember { mutableStateOf(MutableList(maxQuestions.value) { -1 }) }
    val skippedQuestions = remember { mutableStateOf(mutableSetOf<Int>()) }
    val currentQuestion = remember { mutableStateOf(0) }
    val timeLeft = remember { mutableStateOf(timeLimitSeconds.value) }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }
    val unansweredQueue = remember { mutableStateOf<List<Int>>(emptyList()) }
    val showResultDialog = remember { mutableStateOf(false) }
    val isPassed = remember { mutableStateOf(false) }
    val score = remember { mutableStateOf(0) }
    val resume = activity?.intent?.getBooleanExtra("resume", false) ?: false

    val ttsHelper = remember { 
        TTSHelper(context, Locale.getDefault().language).also {
            Log.d("ExamQuestion", "TTSHelper created, ready: ${it.isReady()}")
        }
    }

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
            android.util.Log.e("ExamQuestionActivity", "Error getting localized content for field $field", e)
            data[field] as? String ?: ""
        }
    }

    // Laad voortgang als resume true is
    val pausedQuestionOrder = remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(resume) {
        if (resume) {
            val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            currentQuestion.value = prefs.getInt("paused_current_question", 0)
            selectedAnswers.value = prefs.getString("paused_selected_answers", "")?.split(",")?.map { it.toIntOrNull() ?: -1 }?.toMutableList() ?: MutableList(maxQuestions.value) { -1 }
            skippedQuestions.value = prefs.getString("paused_skipped_questions", "")?.split(",")?.mapNotNull { it.toIntOrNull() }?.toMutableSet() ?: mutableSetOf()
            timeLeft.value = prefs.getInt("paused_time_left", timeLimitSeconds.value)
            pausedQuestionOrder.value = prefs.getString("paused_question_order", null)?.split(",") ?: emptyList()
        }
    }

    // Function to finish exam and show dialog
    val finishExamAndShowDialog = {
        // Score berekenen
        val questionsList = questions.value
        val selected = selectedAnswers.value
        var correct = 0
        questionsList.forEachIndexed { idx, q ->
            if (selected.getOrNull(idx) == q.correctAnswer) correct++
        }
        score.value = correct
        isPassed.value = if (isVolExam.value) correct >= 49 else correct >= 28
        showResultDialog.value = true
        // Sla resultaat op
        saveExamResults(context, examId.value, questionsList, selected, skippedQuestions.value)
    }

    // Firestore ophalen
    LaunchedEffect(examId.value, resume, userLanguage) {
        val db = FirebaseFirestore.getInstance()
        val collectionName = "examenVragen"
        
        // Convert examId to the correct exam type format
        val examTypeFilter = when (examId.value) {
            "basis" -> "vca-basis"
            "vol" -> "vca-vol"
            else -> examId.value
        }
        
        // Filter by exam type using array-contains
        val query = db.collection(collectionName)
            .whereArrayContains("examTypes", examTypeFilter)
            .limit(maxQuestions.value.toLong())
        
        query.get()
            .addOnSuccessListener { querySnapshot ->
                try {
                    Log.d("ExamQuestionActivity", "ExamType: ${examId.value}, Found ${querySnapshot.documents.size} questions")
                    if (querySnapshot.documents.isEmpty()) {
                        Log.e("ExamQuestionActivity", "Geen vragen gevonden voor examType: $examTypeFilter. Controleer Firestore op examTypes-veld en waarde.")
                        // Try without filter to see if there are any questions at all
                        db.collection(collectionName).limit(5).get()
                            .addOnSuccessListener { allQuestionsSnapshot ->
                                Log.d("ExamQuestionActivity", "Found ${allQuestionsSnapshot.documents.size} total questions in collection")
                                if (!allQuestionsSnapshot.isEmpty) {
                                    val sampleDoc = allQuestionsSnapshot.documents[0]
                                    Log.d("ExamQuestionActivity", "Sample document fields: ${sampleDoc.data?.keys}")
                                    Log.d("ExamQuestionActivity", "Sample examTypes: ${sampleDoc.data?.get("examTypes")}")
                                }
                            }
                    }
                    val loadedQuestions = mutableListOf<ExamQuestion>()
                    for (doc in querySnapshot) {
                        val data = doc.data
                        if (data != null) {
                            // Get localized question text
                            val questionText = getLocalizedContent(data, "text", userLanguage)
                            // Get answers and localize them
                            val answersList = data["answers"] as? List<*>
                            val originalOptions = answersList?.mapNotNull { answerObj ->
                                val answer = answerObj as? Map<*, *>
                                if (answer != null) {
                                    // Try to get localized answer text
                                    val answerData = answer as Map<String, Any?>
                                    getLocalizedContent(answerData, "text", userLanguage)
                                } else {
                                    null
                                }
                            } ?: emptyList()
                            val originalCorrectIndex = (data["correctAnswerIndex"] as? Long)?.toInt() ?: 0
                            val correctAnswerText = if (originalCorrectIndex < originalOptions.size) originalOptions[originalCorrectIndex] else ""
                            val shuffledOptions = originalOptions.shuffled()
                            val newCorrectIndex = shuffledOptions.indexOf(correctAnswerText)
                            val imageUrl = data["imageSrc"] as? String
                            // Get localized explanation
                            val explanation = getLocalizedContent(data, "explanation", userLanguage)
                            loadedQuestions.add(
                                ExamQuestion(
                                    id = doc.id,
                                    question = questionText,
                                    options = shuffledOptions,
                                    correctAnswer = newCorrectIndex,
                                    explanation = explanation,
                                    imageUrl = imageUrl
                                )
                            )
                        }
                    }
                    if (resume && pausedQuestionOrder.value.isNotEmpty()) {
                        // Sorteer loadedQuestions volgens pausedQuestionOrder
                        val questionMap = loadedQuestions.associateBy { it.id }
                        val ordered = pausedQuestionOrder.value.mapNotNull { questionMap[it] }
                        questions.value = ordered
                    } else {
                        val shuffled = loadedQuestions.shuffled()
                        questions.value = shuffled
                        // Sla de volgorde op voor pauzeren
                        val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putString("paused_question_order", shuffled.joinToString(",") { it.id }).apply()
                        selectedAnswers.value = MutableList(questions.value.size) { -1 }
                    }
                    isLoading.value = false
                } catch (e: Exception) {
                    android.util.Log.e("ExamQuestionActivity", "Error processing questions", e)
                    hasError.value = true
                    isLoading.value = false
                }
            }
            .addOnFailureListener { 
                android.util.Log.e("ExamQuestionActivity", "Failed to load questions for examType: $examTypeFilter", it)
                hasError.value = true
                isLoading.value = false
            }
    }

    // Timer
    LaunchedEffect(Unit) {
        while (timeLeft.value > 0 && !isLoading.value) {
            delay(1000)
            timeLeft.value -= 1
        }
        if (timeLeft.value == 0) {
            finishExamAndShowDialog()
        }
    }

    if (isLoading.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4B3DFE))
        }
        return
    }
    
    if (hasError.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error loading questions", color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    isLoading.value = true
                    hasError.value = false
                }) {
                    Text("Retry")
                }
            }
        }
        return
    }
    
    if (questions.value.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Geen vragen beschikbaar", fontSize = 18.sp)
        }
        return
    }
    
    val qIndex = currentQuestion.value
    val currentQ = questions.value.getOrNull(qIndex) ?: return
    val totalQuestions = questions.value.size
    val progress = (qIndex + 1).toFloat() / totalQuestions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E7FF))
            .padding(16.dp)
    ) {
        // Header met voortgang
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        examTitle.value,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Vraag ${qIndex + 1} van $totalQuestions",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Timer
                val minutes = timeLeft.value / 60
                val seconds = timeLeft.value % 60
                Text(
                    "Tijd over: ${String.format("%02d:%02d", minutes, seconds)}",
                    fontSize = 14.sp,
                    color = if (timeLeft.value < 300) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Question card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // TTS Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Vraag",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            if (ttsHelper.isReady()) {
                                ttsHelper.speak(currentQ.question)
                            }
                        },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    currentQ.question,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
                
                // Image if available
                currentQ.imageUrl?.let { imageUrl ->
                    Spacer(modifier = Modifier.height(16.dp))
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Question Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Answer options
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            currentQ.options.forEachIndexed { index, option ->
                val isSelected = selectedAnswers.value.getOrNull(qIndex) == index
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedAnswers.value[qIndex] = index
                            skippedQuestions.value.remove(qIndex)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text(
                                    "✓",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            } else {
                                Text(
                                    "${index + 1}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            option,
                            fontSize = 16.sp,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    if (qIndex > 0) {
                        currentQuestion.value = qIndex - 1
                    }
                },
                enabled = qIndex > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (qIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Vorige", color = if (qIndex > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Button(
                onClick = {
                    if (qIndex < totalQuestions - 1) {
                        currentQuestion.value = qIndex + 1
                    } else {
                        finishExamAndShowDialog()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    if (qIndex < totalQuestions - 1) "Volgende" else "Afronden",
                    color = Color.White
                )
            }
        }
    }
    
    // Result dialog
    if (showResultDialog.value) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    if (isPassed.value) "Gefeliciteerd!" else "Helaas...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPassed.value) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            },
            text = {
                Column {
                    Text(
                        "Je score: ${score.value}/${totalQuestions} (${(score.value * 100) / totalQuestions}%)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isPassed.value) "Je bent geslaagd!" else "Je bent niet geslaagd. Probeer het opnieuw!",
                        fontSize = 16.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResultDialog.value = false
                        val intent = Intent(context, ExamResultsActivity::class.java)
                        intent.putExtra("exam_id", examId.value)
                        intent.putExtra("score", score.value)
                        intent.putExtra("total_questions", totalQuestions)
                        intent.putExtra("is_passed", isPassed.value)
                        context.startActivity(intent)
                        activity?.finish()
                    }
                ) {
                    Text("Bekijk Resultaten", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

fun saveExamResults(context: android.content.Context, examId: String, questions: List<ExamQuestion>, selectedAnswers: List<Int>, skippedQuestions: Set<Int>) {
    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    val userEmail = prefs.getString("email", "onbekend") ?: "onbekend"
    
    var correct = 0
    questions.forEachIndexed { idx, q ->
        if (selectedAnswers.getOrNull(idx) == q.correctAnswer) correct++
    }
    
    val result = mapOf(
        "userId" to userEmail,
        "examId" to examId,
        "score" to correct,
        "totalQuestions" to questions.size,
        "selectedAnswers" to selectedAnswers,
        "skippedQuestions" to skippedQuestions.toList(),
        "createdAt" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date()),
        "isPassed" to (if (examId == "vol") correct >= 49 else correct >= 28)
    )
    
    val db = FirebaseFirestore.getInstance()
    db.collection("exam_results")
        .add(result)
        .addOnSuccessListener {
            android.util.Log.d("ExamQuestionActivity", "Exam results saved successfully")
        }
        .addOnFailureListener { e ->
            android.util.Log.e("ExamQuestionActivity", "Failed to save exam results", e)
        }
    // --- LOKAAL OPSLAAN LAATSTE 3 ---
    try {
        val gson = com.google.gson.Gson()
        val existingJson = prefs.getString("local_exam_results", null)
        val resultsList: MutableList<Map<String, Any>> = if (existingJson != null) {
            val type = com.google.gson.reflect.TypeToken.getParameterized(MutableList::class.java, Map::class.java).type
            gson.fromJson(existingJson, type) ?: mutableListOf()
        } else mutableListOf()
        resultsList.add(0, result)
        while (resultsList.size > 3) resultsList.removeLast()
        prefs.edit().putString("local_exam_results", gson.toJson(resultsList)).apply()
    } catch (_: Exception) {}
} 