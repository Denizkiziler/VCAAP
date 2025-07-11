package com.example.vcapp

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vcapp.ui.theme.VCAPPTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlin.random.Random
import java.util.Locale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip

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
                        topicTitle = topicTitle
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
    topicTitle: String
) {
    val context = LocalContext.current
    val ttsHelper = remember { 
        com.example.vcapp.TTSHelper(context, Locale.getDefault().language).also {
            Log.d("TheorieContent", "TTSHelper created, ready: ${it.isReady()}")
        }
    }
    val content = remember { mutableStateOf("") }
    val imageBitmap = remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val question = remember { mutableStateOf<String?>(null) }
    val answers = remember { mutableStateOf<List<String>>(emptyList()) }
    val correctAnswerIndex = remember { mutableStateOf<Int?>(null) }
    val selectedAnswer = remember { mutableStateOf<Int?>(null) }
    val showQuizResult = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }
    val chapterTitle = remember { mutableStateOf("") }
    val subonderwerpen = remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
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
                                val bytes = Base64.decode(base64Clean, Base64.DEFAULT)
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
                                val bytes = Base64.decode(base64Clean, Base64.DEFAULT)
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
            Log.e("TheorieContentActivity", "Error getting localized content for field $field", e)
            data[field] as? String ?: ""
        }
    }

    // Helper function to get localized title from translations
    fun getLocalizedTitle(data: Map<String, Any?>, userLanguage: String): String {
        return try {
            val translations = data["translations"] as? Map<*, *>
            if (translations != null) {
                val langData = translations[userLanguage] as? Map<*, *>
                if (langData != null) {
                    return langData["title"] as? String ?: ""
                }
                // Fallback to Dutch
                val nlData = translations["nl"] as? Map<*, *>
                if (nlData != null) {
                    return nlData["title"] as? String ?: ""
                }
            }
            // Fallback to main field
            data["title"] as? String ?: ""
        } catch (e: Exception) {
            Log.e("TheorieContentActivity", "Error getting localized title", e)
            data["title"] as? String ?: ""
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
        Log.d("TheorieContentActivity", "Loading content for chapter: $chapterId, topic: $topicSlug")
        
        // Get user's language preference
        val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val userLanguage = prefs.getString("lang", Locale.getDefault().language) ?: "nl"
        Log.d("TheorieContentActivity", "User language preference: $userLanguage")
        
        try {
            // First, load the topic document to get its data and check for subonderwerpen
            db.collection("theorieHoofdstukken")
                .document(chapterId)
                .collection("onderwerpen")
                .document(topicSlug)
                .get()
                .addOnSuccessListener { topicDoc ->
                    Log.d("TheorieContentActivity", "Topic document loaded: ${topicDoc.exists()}")
                    
                    if (!topicDoc.exists()) {
                        Log.e("TheorieContentActivity", "Topic document does not exist")
                        hasError.value = true
                        isLoading.value = false
                        return@addOnSuccessListener
                    }
                    
                    val topicData = topicDoc.data
                    if (topicData == null) {
                        Log.e("TheorieContentActivity", "Topic document has no data")
                        hasError.value = true
                        isLoading.value = false
                        return@addOnSuccessListener
                    }
                    
                    // Get chapter title
                    db.collection("theorieHoofdstukken")
                        .document(chapterId)
                        .get()
                        .addOnSuccessListener { chapterDoc ->
                            val chapterData = chapterDoc.data
                            if (chapterData != null) {
                                chapterTitle.value = getLocalizedTitle(chapterData, userLanguage)
                            }
                            
                            // Subonderwerpen als subcollectie ophalen
                            db.collection("theorieHoofdstukken")
                                .document(chapterId)
                                .collection("onderwerpen")
                                .document(topicSlug)
                                .collection("subonderwerpen")
                                .orderBy("order", com.google.firebase.firestore.Query.Direction.ASCENDING)
                                .get()
                                .addOnSuccessListener { subSnapshot ->
                                    try {
                                        Log.d("TheorieContentActivity", "Found ${subSnapshot.documents.size} subonderwerpen in subcollectie")
                                        val subList = subSnapshot.documents.mapNotNull { doc ->
                                            val data = doc.data
                                            if (data != null) {
                                                data.toMutableMap().apply { put("id", doc.id) }
                                            } else null
                                        }
                                        subonderwerpen.value = subList
                                        if (subList.isNotEmpty()) {
                                            // Load first subonderwerp
                                            currentSubIndex.value = 0
                                            val sub = subList[0]
                                            content.value = getLocalizedContent(sub, "content", userLanguage)
                                            Log.d("TheorieContentActivity", "Content length: ${content.value.length}")
                                            val imageSrc = sub["imageSrc"] as? String
                                            val imageData = sub["imageData"] as? String
                                            val imageSource = imageSrc ?: imageData
                                            loadImage(imageSource)
                                            // Load question and answers
                                            val questionData = sub["question"] as? Map<*, *>
                                            question.value = questionData?.get("text") as? String
                                            val answersData = questionData?.get("answers")
                                            val safeAnswersData = when (answersData) {
                                                is List<*> -> answersData.filterIsInstance<Map<String, *>>()
                                                else -> emptyList()
                                            }
                                            val (shuffledAnswers, newCorrectIndex) = shuffleAnswers(safeAnswersData)
                                            answers.value = shuffledAnswers
                                            correctAnswerIndex.value = newCorrectIndex
                                            selectedAnswer.value = null
                                            showQuizResult.value = false
                                        } else {
                                            // Geen subonderwerpen, gebruik topic content
                                            content.value = getLocalizedContent(topicData, "content", userLanguage)
                                            val imageSrc = topicData["imageSrc"] as? String
                                            val imageData = topicData["imageData"] as? String
                                            val imageSource = imageSrc ?: imageData
                                            loadImage(imageSource)
                                            val questionData = topicData["question"] as? Map<*, *>
                                            question.value = questionData?.get("text") as? String
                                            val answersData = questionData?.get("answers")
                                            val safeAnswersData = when (answersData) {
                                                is List<*> -> answersData.filterIsInstance<Map<String, *>>()
                                                else -> emptyList()
                                            }
                                            val (shuffledAnswers, newCorrectIndex) = shuffleAnswers(safeAnswersData)
                                            answers.value = shuffledAnswers
                                            correctAnswerIndex.value = newCorrectIndex
                                            selectedAnswer.value = null
                                            showQuizResult.value = false
                                        }
                                        isLoading.value = false
                                    } catch (e: Exception) {
                                        android.util.Log.e("TheorieContentActivity", "Error loading subonderwerpen from subcollection", e)
                                        hasError.value = true
                                        isLoading.value = false
                                    }
                                }
                                .addOnFailureListener { 
                                    android.util.Log.e("TheorieContentActivity", "Failed to load subonderwerpen subcollection", it)
                                    hasError.value = true
                                    isLoading.value = false 
                                }
                        }
                        .addOnFailureListener { 
                            android.util.Log.e("TheorieContentActivity", "Failed to load chapter", it)
                            hasError.value = true
                            isLoading.value = false 
                        }
                }
                .addOnFailureListener { 
                    android.util.Log.e("TheorieContentActivity", "Failed to load topic", it)
                    hasError.value = true
                    isLoading.value = false 
                }
        } catch (e: Exception) {
            android.util.Log.e("TheorieContentActivity", "Error in LaunchedEffect", e)
            hasError.value = true
            isLoading.value = false
        }
    }

    // Subonderwerp navigatie effect
    fun updateSubIndex(newIndex: Int) {
        val subList = subonderwerpen.value
        if (subList.isNotEmpty() && newIndex in subList.indices) {
            currentSubIndex.value = newIndex
            val sub = subList[newIndex]
            
            // Get user's language preference
            val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            val userLanguage = prefs.getString("lang", java.util.Locale.getDefault().language) ?: "nl"
            
            content.value = getLocalizedContent(sub, "content", userLanguage)
            
            // Check for both imageSrc and imageData fields
            val imageSrc = sub["imageSrc"] as? String
            val imageData = sub["imageData"] as? String
            val imageSource = imageSrc ?: imageData
            loadImage(imageSource)
            
            // Get question and answers
            val questionData = sub["question"] as? Map<*, *>
            question.value = questionData?.get("text") as? String
            
            val answersData = questionData?.get("answers")
            val safeAnswersData = when (answersData) {
                is List<*> -> answersData.filterIsInstance<Map<String, *>>()
                else -> emptyList()
            }
            
            val (shuffledAnswers, newCorrectIndex) = shuffleAnswers(safeAnswersData)
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
                modifier = Modifier.padding(top = 16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    } else if (hasError.value) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error loading content", color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    isLoading.value = true
                    hasError.value = false
                }) {
                    Text("Retry")
                }
            }
        }
    } else {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE0E7FF)) // New background color
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        chapterTitle.value,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        topicTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    // Subonderwerp indicator
                    if (subonderwerpen.value.isNotEmpty()) {
                        Text(
                            "Subonderwerp ${currentSubIndex.value + 1} van ${subonderwerpen.value.size}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            if (content.value.isNotEmpty()) {
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
                                "Content",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    if (ttsHelper.isReady()) {
                                        ttsHelper.speak(content.value)
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
                            content.value,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
            
            // Image
            if (imageBitmap.value != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Image(
                        bitmap = imageBitmap.value!!,
                        contentDescription = "Content Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            // Quiz section
            if (!question.value.isNullOrEmpty() && answers.value.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "Quiz",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Text(
                            question.value!!,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        answers.value.forEachIndexed { index, answer ->
                            Button(
                                onClick = {
                                    selectedAnswer.value = index
                                    showQuizResult.value = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        showQuizResult.value && index == correctAnswerIndex.value -> Color(0xFF4CAF50) // Groen voor juiste antwoord (altijd getoond)
                                        showQuizResult.value && index == selectedAnswer.value && index != correctAnswerIndex.value -> Color(0xFFF44336) // Rood voor fout gekozen antwoord
                                        index == selectedAnswer.value && !showQuizResult.value -> MaterialTheme.colorScheme.primary // Blauw voor gekozen antwoord (voor resultaat)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Text(
                                    answer,
                                    color = when {
                                        showQuizResult.value && index == correctAnswerIndex.value -> Color.White // Wit voor groen juiste antwoord
                                        showQuizResult.value && index == selectedAnswer.value && index != correctAnswerIndex.value -> Color.White // Wit voor rood fout antwoord
                                        index == selectedAnswer.value && !showQuizResult.value -> Color.White // Wit voor blauw gekozen antwoord
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        
                        if (showQuizResult.value) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val isCorrect = selectedAnswer.value == correctAnswerIndex.value
                            val correctAnswerText = answers.value.getOrNull(correctAnswerIndex.value ?: -1) ?: ""
                            Text(
                                if (isCorrect)
                                    "Correct! Goed gedaan! Het juiste antwoord is gemarkeerd in groen."
                                else
                                    "Helaas, dat is niet correct. Het juiste antwoord is gemarkeerd in groen.",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isCorrect)
                                    Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    }
                }
            }
            
            // Subonderwerp navigatie (indien er subonderwerpen zijn)
            if (subonderwerpen.value.size > 1) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "Subonderwerpen",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            subonderwerpen.value.forEachIndexed { index, sub ->
                                val isSelected = index == currentSubIndex.value
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .clickable { updateSubIndex(index) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
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
                    val onderwerpen = remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
                    val currentIndex = remember { mutableStateOf(-1) }
                    val allChapters = remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
                    val currentChapterIndex = remember { mutableStateOf(-1) }
                    
                    LaunchedEffect(chapterId, topicSlug) {
                        val db = FirebaseFirestore.getInstance()
                        
                        // Laad alle hoofdstukken voor navigatie
                        db.collection("theorieHoofdstukken")
                            .orderBy("order", com.google.firebase.firestore.Query.Direction.ASCENDING)
                            .get()
                            .addOnSuccessListener { chaptersSnapshot ->
                                val chaptersList = chaptersSnapshot.documents.mapNotNull { doc ->
                                    val data = doc.data
                                    if (data != null) {
                                        data.toMutableMap().apply { put("id", doc.id) }
                                    } else null
                                }
                                allChapters.value = chaptersList
                                currentChapterIndex.value = chaptersList.indexOfFirst { it["id"] == chapterId }
                            }
                        
                        // Laad onderwerpen van huidige hoofdstuk
                        db.collection("theorieHoofdstukken")
                            .document(chapterId)
                            .collection("onderwerpen")
                            .orderBy("order", com.google.firebase.firestore.Query.Direction.ASCENDING)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val onderwerpenList = snapshot.documents.mapNotNull { doc ->
                                    val data = doc.data
                                    if (data != null) {
                                        data.toMutableMap().apply { put("id", doc.id) }
                                    } else null
                                }
                                onderwerpen.value = onderwerpenList
                                currentIndex.value = onderwerpenList.indexOfFirst { it["id"] == topicSlug }
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
                                    val slug = prevTopic["id"] as? String ?: ""
                                    val title = getLocalizedTitle(prevTopic, "nl") // Use Dutch as fallback
                                    val hasSubtopics = prevTopic["hasSubtopics"] as? Boolean ?: false
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
                                    intent.putExtra("chapter_title", getLocalizedTitle(prevChapter, "nl"))
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
                                    val slug = nextTopic["id"] as? String ?: ""
                                    val title = getLocalizedTitle(nextTopic, "nl") // Use Dutch as fallback
                                    val hasSubtopics = nextTopic["hasSubtopics"] as? Boolean ?: false
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
                                    intent.putExtra("chapter_title", getLocalizedTitle(nextChapter, "nl"))
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
}