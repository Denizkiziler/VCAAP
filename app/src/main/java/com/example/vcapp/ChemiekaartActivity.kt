package com.example.vcapp

import android.os.Bundle
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.vcapp.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ChemiekaartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChemiekaartScreen()
        }
    }
}

data class ChemistryCard(
    val id: String,
    val name: String,
    val formula: String,
    val hazards: List<String>,
    val prevention: List<String>,
    val firstAid: List<String>,
    val category: String,
    val imgSrc: String? = null
)

@Composable
fun ChemiekaartScreen() {
    val context = LocalContext.current
    val chemistryCards = remember { mutableStateOf<List<ChemistryCard>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }
    val selectedCard = remember { mutableStateOf<ChemistryCard?>(null) }
    
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
            android.util.Log.e("ChemiekaartActivity", "Error getting localized content for field $field", e)
            data[field] as? String ?: ""
        }
    }

    LaunchedEffect(Unit) {
        try {
            android.util.Log.d("ChemiekaartActivity", "Loading chemistry cards from Firestore")
            
            // Get user's language preference
            val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
            val userLanguage = prefs.getString("lang", Locale.getDefault().language) ?: "nl"
            android.util.Log.d("ChemiekaartActivity", "User language preference: $userLanguage")
            
            val db = FirebaseFirestore.getInstance()
            db.collection("chemiekaarten")
                .orderBy("name", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    try {
                        android.util.Log.d("ChemiekaartActivity", "Found ${snapshot.documents.size} chemistry card documents")
                        
                        val cardList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data
                                if (data == null) {
                                    android.util.Log.w("ChemiekaartActivity", "Chemistry card document ${doc.id} has no data")
                                    return@mapNotNull null
                                }
                                
                                // Get localized content
                                val name = getLocalizedContent(data, "name", userLanguage)
                                val formula = getLocalizedContent(data, "formula", userLanguage)
                                val category = getLocalizedContent(data, "category", userLanguage)
                                
                                // Get lists
                                val hazards = (data["hazards"] as? List<String>) ?: emptyList()
                                val prevention = (data["prevention"] as? List<String>) ?: emptyList()
                                val firstAid = (data["firstAid"] as? List<String>) ?: emptyList()
                                val imgSrc = data["imgSrc"] as? String
                                
                                android.util.Log.d("ChemiekaartActivity", "Chemistry card: $name")
                                
                                ChemistryCard(
                                    id = doc.id,
                                    name = name,
                                    formula = formula,
                                    hazards = hazards,
                                    prevention = prevention,
                                    firstAid = firstAid,
                                    category = category,
                                    imgSrc = imgSrc
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("ChemiekaartActivity", "Error processing chemistry card ${doc.id}", e)
                                null
                            }
                        }
                        
                        android.util.Log.d("ChemiekaartActivity", "Loaded ${cardList.size} chemistry cards")
                        chemistryCards.value = cardList
                        isLoading.value = false
                        
                    } catch (e: Exception) {
                        android.util.Log.e("ChemiekaartActivity", "Error in Firebase success callback", e)
                        hasError.value = true
                        isLoading.value = false
                    }
                }
                .addOnFailureListener { exception ->
                    android.util.Log.e("ChemiekaartActivity", "Failed to load chemistry cards", exception)
                    hasError.value = true
                    isLoading.value = false
                }
        } catch (e: Exception) {
            android.util.Log.e("ChemiekaartActivity", "Error in LaunchedEffect", e)
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
            // Header
            Text(
                text = "Chemiekaart",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Veiligheidsinformatie over chemische stoffen",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        Text("Error loading chemistry cards", color = MaterialTheme.colorScheme.error)
                        Button(onClick = {
                            isLoading.value = true
                            hasError.value = false
                        }) {
                            Text("Retry")
                        }
                    }
                }
            } else if (chemistryCards.value.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No chemistry cards found")
                }
            } else {
                // Chemistry cards list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chemistryCards.value) { card ->
                        ChemistryCardItem(
                            card = card,
                            onClick = { selectedCard.value = card }
                        )
                    }
                }
            }
        }
        
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
    selectedCard.value?.let { card ->
        ChemistryCardDetailDialog(
            card = card,
            onDismiss = { selectedCard.value = null }
        )
    }
}

@Composable
fun ChemistryCardItem(
    card: ChemistryCard,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chemistry icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF4B3DFE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧪",
                    fontSize = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Card info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = card.formula,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (card.category.isNotEmpty()) {
                    Text(
                        text = card.category,
                        fontSize = 12.sp,
                        color = Color(0xFF4B3DFE),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ChemistryCardDetailDialog(
    card: ChemistryCard,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = card.name,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Formula
                if (card.formula.isNotEmpty()) {
                    Text(
                        text = "Formule:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = card.formula,
                        fontSize = 18.sp,
                        color = Color(0xFF4B3DFE),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                // Hazards
                if (card.hazards.isNotEmpty()) {
                    Text(
                        text = "Gevaren:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFF44336),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    card.hazards.forEach { hazard ->
                        Text(
                            text = "• $hazard",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Prevention
                if (card.prevention.isNotEmpty()) {
                    Text(
                        text = "Preventie:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    card.prevention.forEach { prevention ->
                        Text(
                            text = "• $prevention",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // First Aid
                if (card.firstAid.isNotEmpty()) {
                    Text(
                        text = "Eerste Hulp:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    card.firstAid.forEach { firstAid ->
                        Text(
                            text = "• $firstAid",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B3DFE))
            ) {
                Text("Sluiten", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ChemiekaartScreenPreview() {
    ChemiekaartScreen()
} 