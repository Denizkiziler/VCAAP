package com.example.vcapp

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vcapp.data.MockContentData
import com.example.vcapp.viewmodels.ContentViewModel
import com.example.vcapp.ui.theme.VCAPPTheme
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.example.vcapp.TTSHelper
import java.util.Locale

class TheorieContentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chapterId = intent.getStringExtra("chapter_id") ?: ""
        val topicSlug = intent.getStringExtra("topic_slug") ?: ""
        val topicTitle = intent.getStringExtra("topic_title") ?: "Onderwerp"
        val hasSubtopics = intent.getBooleanExtra("has_subtopics", false)
        setContent {
            VCAPPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TheorieContentScreen(
                        chapterId = chapterId,
                        topicSlug = topicSlug,
                        topicTitle = topicTitle,
                        hasSubtopics = hasSubtopics
                    )
                }
            }
        }
    }
}

@Composable
fun TheorieContentScreen(
    chapterId: String,
    topicSlug: String,
    topicTitle: String,
    hasSubtopics: Boolean
) {
    val context = LocalContext.current
    val ttsHelper = remember { TTSHelper(context, Locale.getDefault().language) }
    val content = remember { mutableStateOf("") }
    val imageBitmap = remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val question = remember { mutableStateOf<String?>(null) }
    val answers = remember { mutableStateOf<List<String>>(emptyList()) }
    val correctAnswerIndex = remember { mutableStateOf<Int?>(null) }
    val selectedAnswer = remember { mutableStateOf<Int?>(null) }
    val showQuizResult = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(true) }
    val chapterTitle = remember { mutableStateOf("") }
    val subonderwerpen = remember { mutableStateOf<List<Map<String, *>>>(emptyList()) }
    val currentSubIndex = remember { mutableStateOf(0) }
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Helper function to load image from various sources
    fun loadImage(imageSource: String?) {
        if (imageSource.isNullOrBlank()) {
            imageBitmap.value = null
            return
        }
        
        when {
            // Firestore imageData reference
            imageSource.startsWith("firestore:imageData/") -> {
                val imageId = imageSource.removePrefix("firestore:imageData/")
                db.collection("imageData").document(imageId).get()
                    .addOnSuccessListener { doc ->
                        val base64 = doc.getString("data")
                        if (!base64.isNullOrBlank()) {
                            try {
                                val base64Clean = base64.substringAfter(",", base64)
                                val bytes = android.util.Base64.decode(base64Clean, android.util.Base64.DEFAULT)
                                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                imageBitmap.value = bmp?.asImageBitmap()
                            } catch (e: Exception) {
                                imageBitmap.value = null
                            }
                        } else {
                            imageBitmap.value = null
                        }
                    }
                    .addOnFailureListener {
                        imageBitmap.value = null
                    }
            }
            
            // Direct imageData ID
            imageSource.matches(Regex("^[a-zA-Z0-9_-]+$")) -> {
                db.collection("imageData").document(imageSource).get()
                    .addOnSuccessListener { doc ->
                        val base64 = doc.getString("data")
                        if (!base64.isNullOrBlank()) {
                            try {
                                val base64Clean = base64.substringAfter(",", base64)
                                val bytes = android.util.Base64.decode(base64Clean, android.util.Base64.DEFAULT)
                                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                imageBitmap.value = bmp?.asImageBitmap()
                            } catch (e: Exception) {
                                imageBitmap.value = null
                            }
                        } else {
                            imageBitmap.value = null
                        }
                    }
                    .addOnFailureListener {
                        imageBitmap.value = null
                    }
            }
            
            // Firebase Storage URL
            imageSource.startsWith("gs://") || imageSource.startsWith("https://firebasestorage.googleapis.com/") -> {
                val storageRef = storage.getReference(imageSource)
                storageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        // For now, we'll set the image to null as we need to handle URL images differently
                        // You might want to use Coil or another image loading library for URLs
                        imageBitmap.value = null
                    }
                    .addOnFailureListener {
                        imageBitmap.value = null
                    }
            }
            
            // Direct URL
            imageSource.startsWith("http") -> {
                // For direct URLs, we'll set to null as we need proper URL image loading
                imageBitmap.value = null
            }
            
            // Unknown format
            else -> {
                imageBitmap.value = null
            }
        }
    }

    // Helper function to get Firebase Storage download URL
    fun getImageDownloadUrl(storagePath: String, onSuccess: (String) -> Unit) {
        if (storagePath.startsWith("gs://")) {
            val storageRef = storage.getReference(storagePath)
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }.addOnFailureListener {
                onSuccess(storagePath)
            }
        } else if (storagePath.startsWith("https://firebasestorage.googleapis.com/")) {
            onSuccess(storagePath)
        } else {
            val storageRef = storage.getReference(storagePath)
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }.addOnFailureListener {
                onSuccess(storagePath)
            }
        }
    }

    // Helper function om antwoorden te shufflen en correct index te bepalen
    fun shuffleAnswers(answersData: List<Map<String, *>>?): Pair<List<String>, Int?> {
        if (answersData == null) return emptyList<String>() to null
        val answerList = answersData.mapNotNull { it["text"] as? String }
        val correctIndex = answersData.indexOfFirst { (it["correct"] as? Boolean) == true }
        if (answerList.isEmpty() || correctIndex == -1) return answerList to null
        val correctAnswer = answerList[correctIndex]
        val shuffled = answerList.shuffled(Random(System.currentTimeMillis()))
        val newCorrectIndex = shuffled.indexOf(correctAnswer)
        return shuffled to newCorrectIndex
    }

    // Laad data
    LaunchedEffect(chapterId, topicSlug) {
        db.collection("theorieHoofdstukken").document(chapterId).get().addOnSuccessListener { doc ->
            chapterTitle.value = doc.getString("title") ?: "Hoofdstuk"
            val onderwerpen = doc["onderwerpen"] as? List<Map<String, *>> ?: emptyList()
            val topic = onderwerpen.find { it["slug"] == topicSlug } ?: return@addOnSuccessListener
            val subList = topic["subonderwerpen"] as? List<Map<String, *>> ?: emptyList()
            subonderwerpen.value = subList
            currentSubIndex.value = 0
            // Init met eerste subonderwerp of topic zelf
            fun loadSub(sub: Map<String, *>) {
                content.value = sub["content"] as? String ?: ""
                
                // Check for both imageSrc and imageData fields
                val imageSrc = sub["imageSrc"] as? String
                val imageData = sub["imageData"] as? String
                val imageSource = imageSrc ?: imageData
                loadImage(imageSource)
                
                val questionData = sub["question"] as? Map<*, *>
                question.value = questionData?.get("text") as? String
                val answersData = questionData?.get("answers")
                val safeAnswersData = (answersData as? List<*>)?.takeIf { it.all { item -> item is Map<*, *> } }?.map { it as Map<String, *> }
                val (shuffledAnswers, newCorrectIndex) = if (safeAnswersData != null) shuffleAnswers(safeAnswersData) else shuffleAnswers(null)
                answers.value = shuffledAnswers
                correctAnswerIndex.value = newCorrectIndex
                selectedAnswer.value = null
                showQuizResult.value = false
            }
            if (subList.isNotEmpty()) {
                loadSub(subList[0])
            } else {
                // Fallback: geen subonderwerpen, gebruik topic zelf
                content.value = topic["content"] as? String ?: ""
                
                // Check for both imageSrc and imageData fields in topic
                val imageSrc = topic["imageSrc"] as? String
                val imageData = topic["imageData"] as? String
                val imageSource = imageSrc ?: imageData
                loadImage(imageSource)
                
                val questionData = topic["question"]
                when (questionData) {
                    is String -> question.value = questionData
                    is Map<*, *> -> question.value =
                        questionData["text"] as? String ?: questionData["question"] as? String

                    else -> question.value = null
                }
                val answersData = topic["answers"]
                val safeAnswersData = (answersData as? List<*>)?.takeIf { it.all { item -> item is Map<*, *> } }?.map { it as Map<String, *> }
                val (shuffledAnswers, newCorrectIndex) = if (safeAnswersData != null) shuffleAnswers(safeAnswersData) else shuffleAnswers(null)
                answers.value = shuffledAnswers
                correctAnswerIndex.value = newCorrectIndex
                selectedAnswer.value = null
                showQuizResult.value = false
            }
            isLoading.value = false
        }.addOnFailureListener { isLoading.value = false }
    }

    // Subonderwerp navigatie effect
    fun updateSubIndex(newIndex: Int) {
        val subList = subonderwerpen.value
        if (subList.isNotEmpty() && newIndex in subList.indices) {
            currentSubIndex.value = newIndex
            val sub = subList[newIndex]
            content.value = sub["content"] as? String ?: ""
            
            // Check for both imageSrc and imageData fields
            val imageSrc = sub["imageSrc"] as? String
            val imageData = sub["imageData"] as? String
            val imageSource = imageSrc ?: imageData
            loadImage(imageSource)
            
            val questionData = sub["question"] as? Map<*, *>
            question.value = questionData?.get("text") as? String
            val answersData = questionData?.get("answers")
            val safeAnswersData = (answersData as? List<*>)?.takeIf { it.all { item -> item is Map<*, *> } }?.map { it as Map<String, *> }
            val (shuffledAnswers, newCorrectIndex) = if (safeAnswersData != null) shuffleAnswers(safeAnswersData) else shuffleAnswers(null)
            answers.value = shuffledAnswers
            correctAnswerIndex.value = newCorrectIndex
            selectedAnswer.value = null
            showQuizResult.value = false
            // Scroll naar boven
            coroutineScope.launch { scrollState.scrollTo(0) }
        }
    }

    if (isLoading.value) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Content laden...",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Alles afspelen knop bovenaan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = {
                val tekst = buildString {
                    append(chapterTitle.value).append(". ")
                    append(topicTitle).append(". ")
                    append(content.value).append(". ")
                    if (!question.value.isNullOrBlank()) {
                        append("Vraag: ").append(question.value).append(". ")
                    }
                    if (answers.value.isNotEmpty()) {
                        append("Antwoorden: ")
                        answers.value.forEachIndexed { i, ans ->
                            append((i+1).toString()).append(": ").append(ans).append(". ")
                        }
                    }
                }
                ttsHelper.speak(tekst)
            }) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Alles afspelen", tint = MaterialTheme.colorScheme.primary)
            }
        }
        // Hoofdstuk header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Text(
                    text = chapterTitle.value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Positie indicator rechtsbovenin
                if (subonderwerpen.value.size > 1) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "${currentSubIndex.value + 1} / ${subonderwerpen.value.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Subonderwerp",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        
        // Afbeelding
        imageBitmap.value?.let { bmp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
        // Onderwerp titel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = topicTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp)
            )
        }
        // Inhoud
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = subonderwerpen.value.getOrNull(currentSubIndex.value)?.get("title") as? String ?: "Inhoud",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = content.value,
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        // Quiz
        if (!question.value.isNullOrBlank() && answers.value.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "🧠 Test je kennis",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = question.value!!,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    answers.value.forEachIndexed { index, answer ->
                        val isSelected = selectedAnswer.value == index
                        val isCorrect = correctAnswerIndex.value == index
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable(enabled = !showQuizResult.value) {
                                    selectedAnswer.value = index
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    showQuizResult.value && isCorrect -> Color(0xFF4CAF50)
                                    showQuizResult.value && isSelected && !isCorrect -> Color(0xFFF44336)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> Color(0xFFF8F9FA)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                2.dp,
                                when {
                                    showQuizResult.value && isCorrect -> Color(0xFF4CAF50)
                                    showQuizResult.value && isSelected && !isCorrect -> Color(0xFFF44336)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> Color(0xFFE9ECEF)
                                }
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected || (showQuizResult.value && (isCorrect || (isSelected && !isCorrect)))) 4.dp else 2.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            when {
                                                showQuizResult.value && isCorrect -> Color.White
                                                showQuizResult.value && isSelected && !isCorrect -> Color.White
                                                isSelected -> Color.White
                                                else -> MaterialTheme.colorScheme.primary
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        showQuizResult.value && isCorrect -> Text(
                                            "✓",
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        showQuizResult.value && isSelected && !isCorrect -> Text(
                                            "✗",
                                            color = Color(0xFFF44336),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        isSelected -> Text(
                                            "✓",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        else -> Text(
                                            "${index + 1}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = answer,
                                    fontSize = 16.sp,
                                    color = when {
                                        showQuizResult.value && isCorrect -> Color.White
                                        showQuizResult.value && isSelected && !isCorrect -> Color.White
                                        isSelected -> Color.White
                                        else -> MaterialTheme.colorScheme.onBackground
                                    },
                                    fontWeight = if (isSelected || (showQuizResult.value && (isCorrect || (isSelected && !isCorrect)))) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showQuizResult.value = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedAnswer.value != null) MaterialTheme.colorScheme.primary else Color(
                                0xFFB8B8B8
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = selectedAnswer.value != null && !showQuizResult.value
                    ) {
                        Text(
                            if (showQuizResult.value) "Resultaat" else "Controleer antwoord",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    if (showQuizResult.value && selectedAnswer.value != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isCorrect = selectedAnswer.value == correctAnswerIndex.value
                        Text(
                            text = if (isCorrect) "✓ Goed!" else "✗ Fout. Het juiste antwoord is: ${
                                answers.value.getOrNull(
                                    correctAnswerIndex.value ?: -1
                                ) ?: "-"
                            }",
                            color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        // Navigatie onderaan (subonderwerpen)
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = LocalContext.current
                // Laad onderwerpen uit Firestore voor hoofdstuk navigatie
                val onderwerpen = remember { mutableStateOf<List<Map<String, *>>>(emptyList()) }
                val currentIndex = remember { mutableStateOf(-1) }
                val allChapters = remember { mutableStateOf<List<Map<String, *>>>(emptyList()) }
                val currentChapterIndex = remember { mutableStateOf(-1) }
                
                LaunchedEffect(chapterId, topicSlug) {
                    val db = FirebaseFirestore.getInstance()
                    
                    // Laad alle hoofdstukken voor navigatie
                    db.collection("theorieHoofdstukken")
                        .orderBy("order", com.google.firebase.firestore.Query.Direction.ASCENDING)
                        .get()
                        .addOnSuccessListener { chaptersSnapshot ->
                            val chaptersList = chaptersSnapshot.documents.mapNotNull { doc ->
                                doc.data?.let { data ->
                                    mapOf(
                                        "id" to doc.id,
                                        "title" to (data["title"] as? String ?: doc.id),
                                        "order" to (data["order"] as? Number ?: 0)
                                    )
                                }
                            }
                            allChapters.value = chaptersList
                            currentChapterIndex.value = chaptersList.indexOfFirst { it["id"] == chapterId }
                        }
                    
                    // Laad onderwerpen van huidige hoofdstuk
                    db.collection("theorieHoofdstukken").document(chapterId).get()
                        .addOnSuccessListener { doc ->
                            val list = doc["onderwerpen"] as? List<Map<String, *>> ?: emptyList()
                            onderwerpen.value = list
                            val idx = list.indexOfFirst { it["slug"] == topicSlug }
                            currentIndex.value = idx
                        }
                }
                
                // Vorige knop
                Button(
                    onClick = {
                        val prevSubIndex = currentSubIndex.value - 1
                        if (prevSubIndex >= 0) {
                            // Ga naar vorige subonderwerp
                            updateSubIndex(prevSubIndex)
                        } else {
                            // Ga naar laatste subonderwerp van vorige onderwerp
                            val prevTopicIdx = currentIndex.value - 1
                            if (prevTopicIdx >= 0) {
                                val prevTopic = onderwerpen.value[prevTopicIdx]
                                val slug = prevTopic["slug"] as? String ?: ""
                                val title = prevTopic["title"] as? String ?: slug
                                val hasSubtopics = (prevTopic["subonderwerpen"] as? List<*>)?.isNotEmpty() == true
                                val intent = android.content.Intent(context, TheorieContentActivity::class.java)
                                intent.putExtra("chapter_id", chapterId)
                                intent.putExtra("topic_slug", slug)
                                intent.putExtra("topic_title", title)
                                intent.putExtra("has_subtopics", hasSubtopics)
                                context.startActivity(intent)
                            } else if (currentChapterIndex.value > 0) {
                                // Ga naar laatste onderwerp van vorige hoofdstuk
                                val prevChapter = allChapters.value[currentChapterIndex.value - 1]
                                val prevChapterId = prevChapter["id"] as? String ?: ""
                                val intent = android.content.Intent(context, ChapterTopicsActivity::class.java)
                                intent.putExtra("chapter_id", prevChapterId)
                                intent.putExtra("chapter_title", prevChapter["title"] as? String ?: "")
                                context.startActivity(intent)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentSubIndex.value > 0 || currentIndex.value > 0 || currentChapterIndex.value > 0) MaterialTheme.colorScheme.primary else Color(
                            0xFFE9ECEF
                        )
                    ),
                    enabled = currentSubIndex.value > 0 || currentIndex.value > 0 || currentChapterIndex.value > 0,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        "← Vorige",
                        color = if (currentSubIndex.value > 0 || currentIndex.value > 0 || currentChapterIndex.value > 0) Color.White else Color(0xFF6C757D),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Home knop in het midden
                IconButton(
                    onClick = {
                        val intent = android.content.Intent(context, DashboardActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Dashboard",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Volgende knop
                Button(
                    onClick = {
                        val nextSubIndex = currentSubIndex.value + 1
                        if (nextSubIndex < subonderwerpen.value.size) {
                            // Ga naar volgend subonderwerp
                            updateSubIndex(nextSubIndex)
                        } else {
                            // Ga naar eerste subonderwerp van volgend onderwerp
                            val nextTopicIdx = currentIndex.value + 1
                            if (nextTopicIdx < onderwerpen.value.size) {
                                val nextTopic = onderwerpen.value[nextTopicIdx]
                                val slug = nextTopic["slug"] as? String ?: ""
                                val title = nextTopic["title"] as? String ?: slug
                                val hasSubtopics = (nextTopic["subonderwerpen"] as? List<*>)?.isNotEmpty() == true
                                val intent = android.content.Intent(context, TheorieContentActivity::class.java)
                                intent.putExtra("chapter_id", chapterId)
                                intent.putExtra("topic_slug", slug)
                                intent.putExtra("topic_title", title)
                                intent.putExtra("has_subtopics", hasSubtopics)
                                context.startActivity(intent)
                            } else if (currentChapterIndex.value >= 0 && currentChapterIndex.value < allChapters.value.size - 1) {
                                // Ga naar eerste onderwerp van volgend hoofdstuk
                                val nextChapter = allChapters.value[currentChapterIndex.value + 1]
                                val nextChapterId = nextChapter["id"] as? String ?: ""
                                val intent = android.content.Intent(context, ChapterTopicsActivity::class.java)
                                intent.putExtra("chapter_id", nextChapterId)
                                intent.putExtra("chapter_title", nextChapter["title"] as? String ?: "")
                                context.startActivity(intent)
                            } else {
                                // Laatste hoofdstuk bereikt, ga naar dashboard
                                val intent = android.content.Intent(context, DashboardActivity::class.java)
                                context.startActivity(intent)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentSubIndex.value < subonderwerpen.value.size - 1 || 
                            currentIndex.value < onderwerpen.value.size - 1 || 
                            (currentChapterIndex.value >= 0 && currentChapterIndex.value < allChapters.value.size - 1))
                            MaterialTheme.colorScheme.primary else Color(0xFFE9ECEF)
                    ),
                    enabled = currentSubIndex.value < subonderwerpen.value.size - 1 || 
                        currentIndex.value < onderwerpen.value.size - 1 || 
                        (currentChapterIndex.value >= 0 && currentChapterIndex.value < allChapters.value.size - 1),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        "Volgende →",
                        color = if (currentSubIndex.value < subonderwerpen.value.size - 1 || 
                            currentIndex.value < onderwerpen.value.size - 1 || 
                            (currentChapterIndex.value >= 0 && currentChapterIndex.value < allChapters.value.size - 1))
                            Color.White else Color(0xFF6C757D),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}