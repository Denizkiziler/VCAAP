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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vcapp.viewmodels.ContentViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import com.google.firebase.firestore.FirebaseFirestore

class ChapterTopicsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chapterId = intent.getStringExtra("chapter_id") ?: ""
        val chapterTitle = intent.getStringExtra("chapter_title") ?: "Hoofdstuk"
        android.util.Log.d("ChapterTopicsActivity", "onCreate - chapterId: $chapterId, chapterTitle: $chapterTitle")
        setContent {
            ChapterTopicsScreen(chapterId = chapterId, chapterTitle = chapterTitle, onTopicClick = { topic, topicTitle, hasSubtopics ->
                val intent = Intent(this, TheorieContentActivity::class.java)
                intent.putExtra("chapter_id", chapterId)
                intent.putExtra("topic_slug", topic)
                intent.putExtra("topic_title", topicTitle)
                intent.putExtra("has_subtopics", hasSubtopics)
                startActivity(intent)
            })
        }
    }
}

@Composable
fun ChapterTopicsScreen(chapterId: String, chapterTitle: String, onTopicClick: (String, String, Boolean) -> Unit) {
    val topics = remember { mutableStateOf<List<Triple<String, String, Boolean>>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }
    val context = LocalContext.current
    
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
            // Fallback to main title field
            data["title"] as? String ?: ""
        } catch (e: Exception) {
            android.util.Log.e("ChapterTopicsActivity", "Error getting localized title", e)
            data["title"] as? String ?: ""
        }
    }
    
    LaunchedEffect(chapterId) {
        android.util.Log.d("ChapterTopicsActivity", "Loading topics for chapter: $chapterId")
        
        // Get user's language preference
        val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val userLanguage = prefs.getString("lang", java.util.Locale.getDefault().language) ?: "nl"
        android.util.Log.d("ChapterTopicsActivity", "User language preference: $userLanguage")
        
        val db = FirebaseFirestore.getInstance()
        
        // Load topics as subcollection instead of array field
        db.collection("theorieHoofdstukken")
            .document(chapterId)
            .collection("onderwerpen")
            .orderBy("order", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    android.util.Log.d("ChapterTopicsActivity", "Found ${snapshot.documents.size} topic documents")
                    
                    val topicList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data
                            if (data == null) {
                                android.util.Log.w("ChapterTopicsActivity", "Topic document ${doc.id} has no data")
                                return@mapNotNull null
                            }
                            
                            val slug = doc.id
                            android.util.Log.d("ChapterTopicsActivity", "Processing topic: $slug")
                            
                            // Get localized title from translations
                            val title = getLocalizedTitle(data, userLanguage)
                            android.util.Log.d("ChapterTopicsActivity", "Topic title: $title")
                            
                            // Check if topic has subonderwerpen subcollection
                            val hasSubtopics = data["hasSubtopics"] as? Boolean ?: false
                            
                            android.util.Log.d("ChapterTopicsActivity", "Topic: $slug - $title (hasSubtopics: $hasSubtopics)")
                            Triple(slug, title, hasSubtopics)
                        } catch (e: Exception) {
                            android.util.Log.e("ChapterTopicsActivity", "Error processing topic ${doc.id}", e)
                            null
                        }
                    }
                    
                    android.util.Log.d("ChapterTopicsActivity", "Loaded ${topicList.size} topics for chapter $chapterId")
                    topics.value = topicList
                    isLoading.value = false
                } catch (e: Exception) {
                    android.util.Log.e("ChapterTopicsActivity", "Error in Firebase success callback", e)
                    hasError.value = true
                    isLoading.value = false
                }
            }
            .addOnFailureListener { 
                android.util.Log.e("ChapterTopicsActivity", "Failed to load topics for chapter $chapterId", it)
                hasError.value = true
                isLoading.value = false 
            }
    }
    
    // Main background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E7FF)) // New background color
    ) {
        // Centered card container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        chapterTitle,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Kies een onderwerp",
                        fontSize = 16.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    if (isLoading.value) {
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
                                Text("Error loading topics", color = MaterialTheme.colorScheme.error)
                                Button(onClick = {
                                    isLoading.value = true
                                    hasError.value = false
                                }) {
                                    Text("Retry")
                                }
                            }
                        }
                    } else if (topics.value.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No topics found")
                        }
                    } else {
                        // Colorful cards for topics
                        val cardColors = listOf(
                            Color(0xFF4F46E5), // Indigo
                            Color(0xFF14B8A6), // Teal
                            Color(0xFFF97316), // Orange
                            Color(0xFFA78BFA), // Purple
                            Color(0xFF0E9488), // Green
                            Color(0xFF6366F1), // Blue
                            Color(0xFFF59E42)  // Light Orange
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            topics.value.forEachIndexed { idx, topic ->
                                val color = cardColors[idx % cardColors.size]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onTopicClick(topic.first, topic.second, topic.third) },
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = color)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp, horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            topic.second,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ChapterTopicsScreenPreview() {
    ChapterTopicsScreen(chapterId = "1", chapterTitle = "Voorbeeld Hoofdstuk") { _, _, _ -> }
} 