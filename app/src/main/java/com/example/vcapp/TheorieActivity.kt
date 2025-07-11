package com.example.vcapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import java.util.Locale
import com.example.vcapp.ui.StatItem

class TheorieActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TheorieOverviewScreen(onChapterClick = { chapterId, chapterTitle ->
                android.util.Log.d("TheorieActivity", "Chapter clicked - ID: $chapterId, Title: $chapterTitle")
                val intent = Intent(this, ChapterTopicsActivity::class.java)
                intent.putExtra("chapter_id", chapterId)
                intent.putExtra("chapter_title", chapterTitle)
                startActivity(intent)
            })
        }
    }
}

@Composable
fun TheorieOverviewScreen(onChapterClick: (String, String) -> Unit) {
    var chapters by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Color palette similar to dashboard
    val cardColors = listOf(
        Color(0xFF4F46E5), // Indigo (Theorie)
        Color(0xFF14B8A6), // Teal (Examen)
        Color(0xFFF97316), // Orange (Begrippen)
        Color(0xFFA78BFA), // Purple (Borden)
        Color(0xFF0E9488), // Green (Chemiekaart)
        Color(0xFF6366F1), // Blue
        Color(0xFFF59E42)  // Light Orange
    )

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
            android.util.Log.e("TheorieActivity", "Error getting localized title", e)
            data["title"] as? String ?: ""
        }
    }

    LaunchedEffect(Unit) {
        try {
            android.util.Log.d("TheorieActivity", "Loading chapters from Firestore")
            
            // Get user's language preference
            val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            val userLanguage = prefs.getString("lang", Locale.getDefault().language) ?: "nl"
            android.util.Log.d("TheorieActivity", "User language preference: $userLanguage")
            
            val db = FirebaseFirestore.getInstance()
            db.collection("theorieHoofdstukken")
                .orderBy("order", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        android.util.Log.d("TheorieActivity", "Found ${snapshot.documents.size} documents")
                        val chapterList = snapshot.documents
                            .mapNotNull { doc ->
                                try {
                                    val data = doc.data
                                    if (data == null) {
                                        android.util.Log.w("TheorieActivity", "Chapter document ${doc.id} has no data")
                                        return@mapNotNull null
                                    }
                                    
                                    // Get localized title from translations
                                    val title = getLocalizedTitle(data, userLanguage)
                                    android.util.Log.d("TheorieActivity", "Chapter: ${doc.id} - $title")
                                    
                                    doc.id to title
                                } catch (e: Exception) {
                                    android.util.Log.e("TheorieActivity", "Error processing chapter ${doc.id}", e)
                                    null
                                }
                            }
                            .distinctBy { it.second } // Filter dubbele titels
                        
                        android.util.Log.d("TheorieActivity", "Loaded ${chapterList.size} unique chapters")
                        chapters = chapterList
                        isLoading = false
                    } catch (e: Exception) {
                        android.util.Log.e("TheorieActivity", "Error in Firebase success callback", e)
                        hasError = true
                        isLoading = false
                    }
                }.addOnFailureListener { 
                    android.util.Log.e("TheorieActivity", "Failed to load chapters", it)
                    hasError = true
                    isLoading = false 
                }
        } catch (e: Exception) {
            android.util.Log.e("TheorieActivity", "Error in LaunchedEffect", e)
            hasError = true
            isLoading = false
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
                    containerColor = Color(0xFF4F46E5)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4F46E5),
                                    Color(0xFF7C3AED)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Theorieoverzicht",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "Kies een hoofdstuk om te leren",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Stats row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            StatItem("Hoofdstukken", "${chapters.size}", Color.White)
                            StatItem("Onderwerpen", "24", Color.White)
                            StatItem("Voortgang", "35%", Color.White)
                        }
                    }
                }
            }
            
            // Content area
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Hoofdstukken laden...",
                                fontSize = 16.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Fout bij laden",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                "Er is een probleem opgetreden bij het laden van de hoofdstukken",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(
                                onClick = {
                                    isLoading = true
                                    hasError = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                            ) {
                                Text("Opnieuw proberen")
                            }
                        }
                    }
                }
            } else if (chapters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Geen hoofdstukken gevonden",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                "Er zijn momenteel geen hoofdstukken beschikbaar",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Colorful cards for chapters
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(chapters.size) { idx ->
                        val (id, title) = chapters[idx]
                        val color = cardColors[idx % cardColors.size]
                        ChapterCard(
                            title = title,
                            color = color,
                            onClick = { onChapterClick(id, title) }
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun ChapterCard(
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Book,
                    contentDescription = "Hoofdstuk",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Klik om te leren",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            // Arrow icon
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.ArrowForward,
                contentDescription = "Ga naar",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

 