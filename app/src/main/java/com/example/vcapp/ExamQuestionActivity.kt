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
    val activity = context as? ComponentActivity
    val examId = remember { mutableStateOf(activity?.intent?.getStringExtra("exam_id") ?: "1") }
    val examTitle = remember { mutableStateOf(activity?.intent?.getStringExtra("exam_title") ?: "VCA Basis Examen") }
    val isVolExam = remember { mutableStateOf(examTitle.value.contains("VOL", ignoreCase = true)) }
    val maxQuestions = remember { mutableStateOf(if (isVolExam.value) 70 else 40) }
    val timeLimitMinutes = remember { mutableStateOf(if (isVolExam.value) 105 else 60) }
    val timeLimitSeconds = remember { mutableStateOf(timeLimitMinutes.value * 60) }

    val questions = remember { mutableStateOf<List<ExamQuestion>>(emptyList()) }
    val selectedAnswers = remember { mutableStateOf(MutableList(maxQuestions.value) { -1 }) }
    val skippedQuestions = remember { mutableStateOf(mutableSetOf<Int>()) }
    val currentQuestion = remember { mutableStateOf(0) }
    val timeLeft = remember { mutableStateOf(timeLimitSeconds.value) }
    val isLoading = remember { mutableStateOf(true) }
    val unansweredQueue = remember { mutableStateOf<List<Int>>(emptyList()) }
    val showResultDialog = remember { mutableStateOf(false) }
    val isPassed = remember { mutableStateOf(false) }
    val score = remember { mutableStateOf(0) }
    val resume = activity?.intent?.getBooleanExtra("resume", false) ?: false

    val ttsHelper = remember { TTSHelper(context, Locale.getDefault().language) }

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
        saveExamResults(context, if (isVolExam.value) "vol" else "basis", questionsList, selected, skippedQuestions.value)
    }

    // Firestore ophalen
    LaunchedEffect(examId.value, resume) {
        val db = FirebaseFirestore.getInstance()
        val collectionName = "examenVragen"
        db.collection(collectionName)
            .limit(maxQuestions.value.toLong())
            .get()
            .addOnSuccessListener { querySnapshot ->
                val loadedQuestions = mutableListOf<ExamQuestion>()
                for (doc in querySnapshot) {
                    val data = doc.data
                    val answersList = data["answers"] as? List<*>
                    val originalOptions = answersList?.mapNotNull { (it as? Map<*, *>)?.get("text") as? String } ?: emptyList()
                    val originalCorrectIndex = (data["correctAnswerIndex"] as? Long)?.toInt() ?: 0
                    val correctAnswerText = if (originalCorrectIndex < originalOptions.size) originalOptions[originalCorrectIndex] else ""
                    val shuffledOptions = originalOptions.shuffled()
                    val newCorrectIndex = shuffledOptions.indexOf(correctAnswerText)
                    val imageUrl = data["imageUrl"] as? String
                    loadedQuestions.add(
                        ExamQuestion(
                            id = doc.id,
                            question = data["text"] as? String ?: "",
                            options = shuffledOptions,
                            correctAnswer = newCorrectIndex,
                            explanation = data["explanation"] as? String ?: "",
                            imageUrl = imageUrl
                        )
                    )
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
            }
            .addOnFailureListener {
                questions.value = emptyList()
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
            .background(Color(0xFFF5F7FA))
            .padding(16.dp)
    ) {
        // Alles afspelen knop bovenaan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = {
                val tekst = buildString {
                    append("Vraag ").append(qIndex + 1).append(" van ").append(totalQuestions).append(". ")
                    append(currentQ.question).append(". ")
                    if (currentQ.options.isNotEmpty()) {
                        append("Antwoorden: ")
                        currentQ.options.forEachIndexed { i, ans ->
                            append((i+1).toString()).append(": ").append(ans).append(". ")
                        }
                    }
                }
                ttsHelper.speak(tekst)
            }) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Alles afspelen", tint = Color(0xFF4B3DFE))
            }
        }
        // Topbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Examen", fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f))
            // Timer badge
            Box(
                modifier = Modifier
                    .background(Color(0xFFEEE6FF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = String.format("\u23F1 %02d:%02d", timeLeft.value / 60, timeLeft.value % 60),
                    color = Color(0xFF4B3DFE),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Color(0xFF4B3DFE),
            trackColor = Color(0xFFE0E0E0)
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Vraagnummer
        Text("Vraag ${qIndex + 1} van $totalQuestions", fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        // Vraagtekst
        if (!currentQ.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = currentQ.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(bottom = 12.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        Text(currentQ.question, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        // Antwoordopties
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            currentQ.options.forEachIndexed { index, option ->
                val selected = selectedAnswers.value[qIndex] == index
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedAnswers.value = selectedAnswers.value.toMutableList().apply { this[qIndex] = index }
                            skippedQuestions.value.remove(qIndex)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) Color(0xFF4B3DFE) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.White,
                                unselectedColor = if (selected) Color.White else Color(0xFF4B3DFE)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option,
                            color = if (selected) Color.White else Color(0xFF222222),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        // Navigatieknoppen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    skippedQuestions.value.add(qIndex)
                    if (!unansweredQueue.value.contains(qIndex)) {
                        unansweredQueue.value = unansweredQueue.value + qIndex
                    }
                    // Spring naar volgende onbeantwoorde vraag
                    val nextUnanswered = selectedAnswers.value.indexOfFirst { it == -1 && it != qIndex }
                    if (nextUnanswered != -1) {
                        currentQuestion.value = nextUnanswered
                    } else {
                        finishExamAndShowDialog()
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Overslaan", color = Color(0xFF4B3DFE), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    // Spring naar volgende onbeantwoorde vraag
                    val nextUnanswered = selectedAnswers.value.indexOfFirst { it == -1 && it != qIndex }
                    if (nextUnanswered != -1) {
                        currentQuestion.value = nextUnanswered
                    } else {
                        finishExamAndShowDialog()
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3DFE)),
                enabled = selectedAnswers.value[qIndex] != -1
            ) {
                val isLastUnanswered = selectedAnswers.value.count { it == -1 } <= 1
                Text(if (isLastUnanswered) "Afronden" else "Volgende", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        // Navigatie voor overgeslagen vragen onderaan
        if (skippedQuestions.value.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                skippedQuestions.value.sorted().forEach { qIndex ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4B3DFE))
                            .clickable { currentQuestion.value = qIndex },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${qIndex + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
        // Pauzeren-knop
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    // Sla voortgang op in SharedPreferences
                    val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.putString("paused_exam_id", examId.value)
                    editor.putString("paused_exam_title", examTitle.value)
                    editor.putInt("paused_current_question", currentQuestion.value)
                    editor.putString("paused_selected_answers", selectedAnswers.value.joinToString(","))
                    editor.putString("paused_skipped_questions", skippedQuestions.value.joinToString(","))
                    editor.putInt("paused_time_left", timeLeft.value)
                    editor.putString("paused_question_order", questions.value.joinToString(",") { it.id })
                    editor.apply()
                    // Ga naar dashboard
                    context.startActivity(Intent(context, DashboardActivity::class.java))
                    activity?.finish()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB8B8B8)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("Pauzeren", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Resultaat-popup
    if (showResultDialog.value) {
        AlertDialog(
            onDismissRequest = {},
            title = null,
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(if (isPassed.value) Color(0xFF4CAF50) else Color(0xFFF44336)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPassed.value) "🎉" else "✗",
                            fontSize = 40.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isPassed.value) "GESLAAGD!" else "GEZAKT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = if (isPassed.value) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val percentage = if (questions.value.isNotEmpty()) (score.value * 100) / questions.value.size else 0
                    Text(
                        text = "Score: ${score.value} van ${questions.value.size} ($percentage%)",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fout: ${questions.value.size - score.value}",
                        fontSize = 18.sp,
                        color = Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isPassed.value) {
                        Text(
                            text = "Gefeliciteerd met je resultaat!",
                            fontSize = 16.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        "Wil je de resultaten bekijken, een nieuw examen starten of terug naar het hoofdmenu?",
                        fontSize = 16.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showResultDialog.value = false
                    context.startActivity(Intent(context, ExamResultsActivity::class.java))
                    activity?.finish()
                }) {
                    Text("Bekijk resultaten", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showResultDialog.value = false
                        context.startActivity(Intent(context, ExamOverviewActivity::class.java))
                        activity?.finish()
                    }) {
                        Text("Nieuw examen")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        showResultDialog.value = false
                        context.startActivity(Intent(context, DashboardActivity::class.java))
                        activity?.finish()
                    }) {
                        Text("Hoofdmenu")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

private fun saveExamResults(
    context: android.content.Context,
    examType: String,
    questions: List<ExamQuestion>,
    selectedAnswers: List<Int>,
    skippedQuestions: Set<Int>
) {
    val db = FirebaseFirestore.getInstance()
    
    // Calculate score
    var correctAnswers = 0
    val questionResults = mutableListOf<Map<String, Any>>()
    
    questions.forEachIndexed { index, question ->
        val userAnswer = selectedAnswers.getOrNull(index) ?: -1
        val isCorrect = userAnswer == question.correctAnswer
        val isSkipped = skippedQuestions.contains(index)
        
        if (isCorrect && !isSkipped) {
            correctAnswers++
        }
        
        questionResults.add(
            mapOf(
                "questionId" to question.id,
                "question" to question.question,
                "userAnswer" to userAnswer,
                "correctAnswer" to question.correctAnswer,
                "isCorrect" to isCorrect,
                "isSkipped" to isSkipped,
                "explanation" to question.explanation,
                "options" to question.options
            )
        )
    }
    
    val score = correctAnswers
    val totalQuestions = questions.size
    // Hardcoded passing scores: VCA Basis = 28/40 (70%), VCA VOL = 49/70 (70%)
    val passScore = if (examType == "vol") 49 else 28
    val isPassed = score >= passScore
    
    val result = mapOf(
        "examType" to examType,
        "score" to score,
        "totalQuestions" to totalQuestions,
        "passScore" to passScore,
        "isPassed" to isPassed,
        "skippedQuestions" to skippedQuestions.size,
        "questionResults" to questionResults,
        "createdAt" to Date().time // als timestamp
    )
    
    db.collection("exam_results")
        .add(result)
        .addOnSuccessListener {
            // Result saved successfully
        }
        .addOnFailureListener {
            // Handle error
        }

    // Sla lokaal op (laatste 3 resultaten)
    try {
        val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val gson = Gson()
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

private fun getMockQuestions(): List<ExamQuestion> {
    return listOf(
        ExamQuestion(
            id = "1",
            question = "Wat is het belangrijkste doel van VCA?",
            options = listOf("Kosten besparen", "Veilig werken", "Sneller werken", "Minder personeel"),
            correctAnswer = 1,
            explanation = "VCA staat voor Veiligheid, Gezondheid en Milieu Checklist Aannemers en heeft als hoofddoel veilig werken."
        ),
        ExamQuestion(
            id = "2",
            question = "Welke kleur heeft een veiligheidssignaal?",
            options = listOf("Rood", "Blauw", "Groen", "Geel"),
            correctAnswer = 2,
            explanation = "Veiligheidssignalen zijn groen van kleur en geven aan dat iets veilig is."
        ),
        ExamQuestion(
            id = "3",
            question = "Wat moet je doen bij een brand?",
            options = listOf("Water gebruiken", "Vluchten", "Blussen met de hand", "Niets doen"),
            correctAnswer = 1,
            explanation = "Bij brand moet je altijd eerst vluchten en de brandweer waarschuwen."
        )
    )
}

@Composable
fun showDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Afronden") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
} 