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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.vcapp.ui.CategoryChip
import com.example.vcapp.ui.StatItem
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class BordenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BordenScreen()
        }
    }
}

data class SafetySign(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val color: Color,
    val icon: String,
    val meaning: String,
    val examples: List<String>,
    val imgSrc: String? = null
)

data class SignCategory(
    val id: String,
    val name: String,
    val description: String,
    val color: Color,
    val signs: List<SafetySign>
)

@Composable
fun BordenScreen() {
    val context = LocalContext.current
    val selectedCategory = remember { mutableStateOf("Alle") }
    val selectedSign = remember { mutableStateOf<SafetySign?>(null) }
    val categories = remember { mutableStateOf<List<SignCategory>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }
    
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
            android.util.Log.e("BordenActivity", "Error getting localized content for field $field", e)
            data[field] as? String ?: ""
        }
    }
    
    // Helper function to get color from category
    fun getCategoryColor(categoryName: String): Color {
        return when (categoryName.lowercase()) {
            "verbod" -> Color(0xFFE53935)
            "waarschuwing" -> Color(0xFFFF9800)
            "verplichting" -> Color(0xFF2196F3)
            "veiligheid" -> Color(0xFF4CAF50)
            "brandbestrijding" -> Color(0xFFF44336)
            else -> Color(0xFF6366F1)
        }
    }

    LaunchedEffect(Unit) {
        try {
            android.util.Log.d("BordenActivity", "Loading sign categories from Firestore")
            
            // Get user's language preference
            val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            val userLanguage = prefs.getString("lang", Locale.getDefault().language) ?: "nl"
            android.util.Log.d("BordenActivity", "User language preference: $userLanguage")
            
            val db = FirebaseFirestore.getInstance()
            db.collection("bordenCategorieen")
                .orderBy("order", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        android.util.Log.d("BordenActivity", "Found ${snapshot.documents.size} category documents")
                        
                        val categoryList = mutableListOf<SignCategory>()
                        
                        snapshot.documents.forEach { categoryDoc ->
                            try {
                                val categoryData = categoryDoc.data
                                if (categoryData == null) {
                                    android.util.Log.w("BordenActivity", "Category document ${categoryDoc.id} has no data")
                                    return@forEach
                                }
                                
                                val categoryName = getLocalizedContent(categoryData, "name", userLanguage)
                                val categoryDescription = getLocalizedContent(categoryData, "description", userLanguage)
                                val categoryColor = getCategoryColor(categoryName)
                                
                                android.util.Log.d("BordenActivity", "Processing category: $categoryName")
                                
                                // Load signs for this category
                                val signs = categoryData["signs"] as? List<Map<String, Any?>> ?: emptyList()
                                val signList = signs.mapNotNull { signData ->
                                    try {
                                        val signName = getLocalizedContent(signData, "name", userLanguage)
                                        val signDescription = getLocalizedContent(signData, "description", userLanguage)
                                        val signMeaning = getLocalizedContent(signData, "meaning", userLanguage)
                                        val signExamples = (signData["examples"] as? List<String>) ?: emptyList()
                                        val signImgSrc = signData["imgSrc"] as? String
                                        
                                        SafetySign(
                                            id = signData["id"] as? String ?: "",
                                            name = signName,
                                            description = signDescription,
                                            category = categoryName,
                                            color = categoryColor,
                                            icon = signData["icon"] as? String ?: "⚠️",
                                            meaning = signMeaning,
                                            examples = signExamples,
                                            imgSrc = signImgSrc
                                        )
                                    } catch (e: Exception) {
                                        android.util.Log.e("BordenActivity", "Error processing sign in category $categoryName", e)
                                        null
                                    }
                                }
                                
                                categoryList.add(
                                    SignCategory(
                                        id = categoryDoc.id,
                                        name = categoryName,
                                        description = categoryDescription,
                                        color = categoryColor,
                                        signs = signList
                                    )
                                )
                                
                            } catch (e: Exception) {
                                android.util.Log.e("BordenActivity", "Error processing category ${categoryDoc.id}", e)
                            }
                        }
                        
                        android.util.Log.d("BordenActivity", "Loaded ${categoryList.size} categories with ${categoryList.sumOf { it.signs.size }} total signs")
                        categories.value = categoryList
                        isLoading.value = false
                        
                    } catch (e: Exception) {
                        android.util.Log.e("BordenActivity", "Error in Firebase success callback", e)
                        hasError.value = true
                        isLoading.value = false
                    }
                }
                .addOnFailureListener { 
                    android.util.Log.e("BordenActivity", "Failed to load sign categories", it)
                    hasError.value = true
                    isLoading.value = false 
                }
        } catch (e: Exception) {
            android.util.Log.e("BordenActivity", "Error in LaunchedEffect", e)
            hasError.value = true
            isLoading.value = false
        }
    }
    
    val allSigns = categories.value.flatMap { it.signs }
    val categoryNames = listOf("Alle") + categories.value.map { it.name }
    val filteredSigns = if (selectedCategory.value == "Alle") {
        allSigns
    } else {
        allSigns.filter { it.category == selectedCategory.value }
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
                    containerColor = Color(0xFFA78BFA)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFA78BFA),
                                    Color(0xFF8B5CF6)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Veiligheidsborden",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "Leer de betekenis van veiligheidsborden",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Stats row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Sval isCorrect = userAnswer . trim ().equals(
                                correctAnswer,
                                ignoreCase = true
                            )
                            // ... andere code ...

                            if (!isCorrect) { // Als je hier per ongeluk !isCorrect hebt staan
                                showFeedback("Fout!")
                            } else {
                                showFeedback("Goed!")
                            }
                            val isCorrect =
                                userAnswer.trim().equals(correctAnswer, ignoreCase = true)
                            // ... andere code ...

                            if (!isCorrect) { // Als je hier per ongeluk !isCorrect hebt staan
                                showFeedback("Fout!")
                            } else {
                                showFeedback("Goed!")
                            }
                            val correctAnswer = "Parijs"
                            val userAnswer = "parijs " // Antwoord van de gebruiker

                            // Foutgevoelige vergelijking:
                            // if (userAnswer == correctAnswer) { ... } // Dit zou false zijn

                            // Betere vergelijking:
                            if (userAnswer.trim().equals(correctAnswer, ignoreCase = true)) {
                                // Antwoord is correct
                            } else {
                                // Antwoord is fout
                            }
                            val correctAnswer = "Parijs"
                            val userAnswer = "parijs " // Antwoord van de gebruiker

                            // Foutgevoelige vergelijking:
                            // if (userAnswer == correctAnswer) { ... } // Dit zou false zijn

                            // Betere vergelijking:
                            if (userAnswer.trim().equals(correctAnswer, ignoreCase = true)) {
                                // Antwoord is correct
                            } else {
                                // Antwoord is fout
                            }
                            val correctAnswer = "Parijs"
                            val userAnswer = "parijs " // Antwoord van de gebruiker

                            // Foutgevoelige vergelijking:
                            // if (userAnswer == correctAnswer) { ... } // Dit zou false zijn

                            // Betere vergelijking:
                            if (userAnswer.trim().equals(correctAnswer, ignoreCase = true)) {
                                // Antwoord is correct
                            } else {
                                // Antwoord is fout
                            }
                            val correctAnswer = "Parijs"
                            val userAnswer = "parijs " // Antwoord van de gebruiker

                            // Foutgevoelige vergelijking:
                            // if (userAnswer == correctAnswer) { ... } // Dit zou false zijn

                            // Betere vergelijking:
                            if (userAnswer.trim().equals(correctAnswer, ignoreCase = true)) {
                                // Antwoord is correct
                            } else {
                                // Antwoord is fout
                            }tatItem("Categorieën", "${categories.value.size}", Color.White)
                            StatItem("Borden", "${allSigns.size}", Color.White)
                            StatItem("Gefilterd", "${filteredSigns.size}", Color.White)
                        }
                    }
                }
            }
        
        if (isLoading.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (hasError.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error loading sign categories", color = MaterialTheme.colorScheme.error)
                    Button(onClick = {
                        isLoading.value = true
                        hasError.value = false
                    }) {
                        Text("Retry")
                    }
                }
            }
        } else if (categories.value.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No sign categories found")
            }
        } else {
            // Category filter
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryNames.toList()) { category ->
                        CategoryChip(
                            category = category,
                            isSelected = selectedCategory.value == category,
                            onClick = { selectedCategory.value = category }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Signs grid
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSigns) { sign ->
                    SignCard(
                        sign = sign,
                        onClick = { selectedSign.value = sign }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.fillMaxWidth())
        
        // Dashboard button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = {
                    context.startActivity(android.content.Intent(context, DashboardActivity::class.java))
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3DFE))
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Dashboard",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
    
    // Detail dialog
    selectedSign.value?.let { sign ->
        SignDetailDialog(
            sign = sign,
            onDismiss = { selectedSign.value = null }
        )
    }
}

@Composable
fun SignCard(
    sign: SafetySign,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sign icon or image
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(sign.color),
                contentAlignment = Alignment.Center
            ) {
                if (sign.imgSrc != null) {
                    // TODO: Load image from imgSrc using Coil or similar
                    Text(
                        text = sign.icon,
                        fontSize = 24.sp
                    )
                } else {
                    Text(
                        text = sign.icon,
                        fontSize = 24.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Sign info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sign.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = sign.description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = sign.category,
                    fontSize = 12.sp,
                    color = sign.color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SignDetailDialog(
    sign: SafetySign,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sign icon or image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(sign.color),
                contentAlignment = Alignment.Center
            ) {
                if (sign.imgSrc != null) {
                    // TODO: Load image from imgSrc using Coil or similar
                    Text(
                        text = sign.icon,
                        fontSize = 32.sp
                    )
                } else {
                    Text(
                        text = sign.icon,
                        fontSize = 32.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = sign.name,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = sign.meaning,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (sign.examples.isNotEmpty()) {
                Text(
                    text = "Voorbeelden van gebruik:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                sign.examples.forEach { example ->
                    Text(
                        text = "• $example",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3DFE))
            ) {
                Text("Sluiten", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun BordenScreenPreview() {
    BordenScreen()
} 