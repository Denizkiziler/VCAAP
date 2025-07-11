package com.example.vcapp

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vcapp.viewmodels.ContentViewModel
import com.example.vcapp.ui.CategoryChip
import com.example.vcapp.ui.theme.VCAPPTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.example.vcapp.TTSHelper
import java.util.*
import android.util.Log
import java.util.Locale

class BegrippenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VCAPPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BegrippenScreen()
                }
            }
        }
    }
}

@Composable
fun BegrippenScreen() {
    val context = LocalContext.current
    val ttsHelper = remember { 
        TTSHelper(context, Locale.getDefault().language).also {
            Log.d("Begrippen", "TTSHelper created, ready: ${it.isReady()}")
        }
    }
    val searchQuery = remember { mutableStateOf("") }
    val terms = remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()

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
            android.util.Log.e("BegrippenActivity", "Error getting localized content for field $field", e)
            data[field] as? String ?: ""
        }
    }

    LaunchedEffect(Unit) {
        try {
            android.util.Log.d("BegrippenActivity", "Loading terms from Firestore")
            
            // Get user's language preference
            val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            val userLanguage = prefs.getString("lang", Locale.getDefault().language) ?: "nl"
            android.util.Log.d("BegrippenActivity", "User language preference: $userLanguage")
            
            db.collection("afkortingen")
                .orderBy("term", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        android.util.Log.d("BegrippenActivity", "Found ${snapshot.documents.size} term documents")
                        
                        val termList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data
                                if (data == null) {
                                    android.util.Log.w("BegrippenActivity", "Term document ${doc.id} has no data")
                                    return@mapNotNull null
                                }
                                
                                // Get localized content
                                val termText = getLocalizedContent(data, "term", userLanguage)
                                val definition = getLocalizedContent(data, "definition", userLanguage)
                                val uitleg = getLocalizedContent(data, "uitleg", userLanguage)
                                val category = getLocalizedContent(data, "category", userLanguage)
                                
                                android.util.Log.d("BegrippenActivity", "Term: $termText")
                                
                                data.toMutableMap().apply {
                                    put("id", doc.id)
                                    put("term", termText)
                                    put("definition", definition)
                                    put("uitleg", uitleg)
                                    put("category", category)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("BegrippenActivity", "Error processing term ${doc.id}", e)
                                null
                            }
                        }
                        
                        android.util.Log.d("BegrippenActivity", "Loaded ${termList.size} terms")
                        terms.value = termList
                        isLoading.value = false
                        
                    } catch (e: Exception) {
                        android.util.Log.e("BegrippenActivity", "Error in Firebase success callback", e)
                        hasError.value = true
                        isLoading.value = false
                    }
                }
                .addOnFailureListener { exception ->
                    android.util.Log.e("BegrippenActivity", "Failed to load terms", exception)
                    hasError.value = true
                    isLoading.value = false
                }
        } catch (e: Exception) {
            android.util.Log.e("BegrippenActivity", "Error in LaunchedEffect", e)
            hasError.value = true
            isLoading.value = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E7FF)) // New background color
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Alles afspelen knop bovenaan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = {
                    Log.d("Begrippen", "TTS button clicked")
                    val tekst = buildString {
                        val filteredTerms = terms.value.filter { term ->
                            val termText = term["term"] as? String ?: ""
                            val definitionText = term["definition"] as? String ?: ""
                            val searchLower = searchQuery.value.lowercase()
                            termText.lowercase().contains(searchLower) || 
                            definitionText.lowercase().contains(searchLower)
                        }
                        val sortedTerms = filteredTerms.sortedBy { (it["term"] as? String)?.lowercase() ?: "" }
                        sortedTerms.forEach { term ->
                            val termText = term["term"] as? String ?: ""
                            val definition = term["definition"] as? String ?: ""
                            val uitleg = term["uitleg"] as? String ?: ""
                            append(termText).append(". ")
                            append(definition).append(". ")
                            if (uitleg.isNotBlank()) append(uitleg).append(". ")
                        }
                    }
                    Log.d("Begrippen", "Speaking text: ${tekst.take(100)}...")
                    ttsHelper.speak(tekst)
                }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Alles afspelen", tint = MaterialTheme.colorScheme.primary)
                }
            }
            // Home button bovenaan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = {
                        val intent = android.content.Intent(context, DashboardActivity::class.java)
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            // Header
            Text(
                text = "VCA Begrippen",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Zoek en leer belangrijke VCA termen en definities",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Enhanced search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery.value,
                    onValueChange = { searchQuery.value = it },
                    label = { 
                        Text(
                            text = if (searchQuery.value.isEmpty()) "Zoek in begrippen en definities..." else "Zoeken..."
                        ) 
                    },
                    placeholder = { 
                        Text(
                            text = "Bijvoorbeeld: VCA, PBM, veiligheid...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = "Zoeken",
                            tint = if (searchQuery.value.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.value.isNotEmpty()) {
                            IconButton(onClick = { searchQuery.value = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Wissen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Content
            when {
                isLoading.value -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                hasError.value -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error loading terms", color = MaterialTheme.colorScheme.error)
                            Button(onClick = {
                                isLoading.value = true
                                hasError.value = false
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                terms.value.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No terms found")
                    }
                }
                else -> {
                    val filteredTerms = terms.value.filter { term ->
                        val termText = term["term"] as? String ?: ""
                        val definitionText = term["definition"] as? String ?: ""
                        val searchLower = searchQuery.value.lowercase()
                        termText.lowercase().contains(searchLower) || 
                        definitionText.lowercase().contains(searchLower)
                    }
                    // Sorteer alfabetisch, ongeacht hoofdletters
                    val sortedTerms = filteredTerms.sortedBy { (it["term"] as? String)?.lowercase() ?: "" }

                    if (sortedTerms.isEmpty()) {
                        // Toon helemaal niets als er geen begrippen zijn
                        Spacer(modifier = Modifier.height(0.dp))
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(sortedTerms) { term ->
                                TermCard(term = term)
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = {
                val intent = android.content.Intent(context, DashboardActivity::class.java)
                context.startActivity(intent)
                (context as? android.app.Activity)?.finish()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Home, contentDescription = "Home", tint = Color.White)
        }
    }
}

@Composable
fun TermCard(term: Map<String, Any>) {
    val termText = term["term"] as? String ?: ""
    val definition = term["definition"] as? String ?: ""
    val category = term["category"] as? String ?: ""
    val uitleg = term["uitleg"] as? String ?: ""

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
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = termText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (category.isNotEmpty()) {
                        Card(
                            modifier = Modifier.padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = definition,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(bottom = if (uitleg.isNotBlank()) 8.dp else 0.dp)
                    )
                    if (uitleg.isNotBlank()) {
                        Text(
                            text = uitleg,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BegrippenScreenPreview() {
    BegrippenScreen()
} 