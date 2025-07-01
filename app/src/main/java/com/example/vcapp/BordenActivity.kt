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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val examples: List<String>
)

@Composable
fun BordenScreen() {
    val selectedCategory = remember { mutableStateOf("Alle") }
    val selectedSign = remember { mutableStateOf<SafetySign?>(null) }
    
    val categories = listOf("Alle", "Verbod", "Waarschuwing", "Verplichting", "Veiligheid", "Brandbestrijding")
    
    val safetySigns = listOf(
        SafetySign(
            id = "1",
            name = "Roken Verboden",
            description = "Verbod op roken",
            category = "Verbod",
            color = Color(0xFFE53935),
            icon = "🚭",
            meaning = "Roken is hier verboden vanwege brandgevaar of explosiegevaar.",
            examples = listOf("Tankstations", "Opslagplaatsen", "Chemische ruimtes")
        ),
        SafetySign(
            id = "2",
            name = "Open Vuur Verboden",
            description = "Verbod op open vuur",
            category = "Verbod",
            color = Color(0xFFE53935),
            icon = "🔥",
            meaning = "Open vuur is verboden vanwege brandgevaar.",
            examples = listOf("Opslagplaatsen", "Tankstations", "Chemische ruimtes")
        ),
        SafetySign(
            id = "3",
            name = "Geen Toegang",
            description = "Toegang verboden",
            category = "Verbod",
            color = Color(0xFFE53935),
            icon = "🚫",
            meaning = "Toegang is verboden voor onbevoegden.",
            examples = listOf("Machinekamers", "Elektrische ruimtes", "Opslagplaatsen")
        ),
        SafetySign(
            id = "4",
            name = "Gevaar voor Elektriciteit",
            description = "Gevaar voor elektrische schok",
            category = "Waarschuwing",
            color = Color(0xFFFF9800),
            icon = "⚡",
            meaning = "Gevaar voor elektrische schokken. Voorzichtig zijn.",
            examples = listOf("Elektrische kasten", "Hoogspanningsinstallaties", "Machinekamers")
        ),
        SafetySign(
            id = "5",
            name = "Gevaar voor Giftige Stoffen",
            description = "Gevaar voor giftige stoffen",
            category = "Waarschuwing",
            color = Color(0xFFFF9800),
            icon = "☠️",
            meaning = "Gevaar voor giftige of schadelijke stoffen.",
            examples = listOf("Chemische opslag", "Laboratoria", "Industriële ruimtes")
        ),
        SafetySign(
            id = "6",
            name = "Gevaar voor Vallen",
            description = "Gevaar voor vallen",
            category = "Waarschuwing",
            color = Color(0xFFFF9800),
            icon = "⚠️",
            meaning = "Gevaar voor vallen van hoogte.",
            examples = listOf("Steigers", "Daken", "Ladders")
        ),
        SafetySign(
            id = "7",
            name = "Veiligheidshelm Verplicht",
            description = "Veiligheidshelm dragen verplicht",
            category = "Verplichting",
            color = Color(0xFF2196F3),
            icon = "⛑️",
            meaning = "Het dragen van een veiligheidshelm is verplicht.",
            examples = listOf("Bouwplaatsen", "Industriële ruimtes", "Opslagplaatsen")
        ),
        SafetySign(
            id = "8",
            name = "Veiligheidsbril Verplicht",
            description = "Veiligheidsbril dragen verplicht",
            category = "Verplichting",
            color = Color(0xFF2196F3),
            icon = "👓",
            meaning = "Het dragen van een veiligheidsbril is verplicht.",
            examples = listOf("Laswerk", "Slijpwerk", "Chemische werkzaamheden")
        ),
        SafetySign(
            id = "9",
            name = "Handen Wassen",
            description = "Handen wassen verplicht",
            category = "Verplichting",
            color = Color(0xFF2196F3),
            icon = "🧼",
            meaning = "Handen wassen is verplicht voor hygiëne.",
            examples = listOf("Keukens", "Laboratoria", "Medische ruimtes")
        ),
        SafetySign(
            id = "10",
            name = "Eerste Hulp",
            description = "Eerste hulp post",
            category = "Veiligheid",
            color = Color(0xFF4CAF50),
            icon = "🏥",
            meaning = "Locatie van eerste hulp post of EHBO-kit.",
            examples = listOf("Kantoren", "Fabrieken", "Bouwplaatsen")
        ),
        SafetySign(
            id = "11",
            name = "Nooduitgang",
            description = "Nooduitgang",
            category = "Veiligheid",
            color = Color(0xFF4CAF50),
            icon = "🚪",
            meaning = "Nooduitgang voor evacuatie bij gevaar.",
            examples = listOf("Gebouwen", "Tunnels", "Industriële ruimtes")
        ),
        SafetySign(
            id = "12",
            name = "Brandblusser",
            description = "Brandblusser",
            category = "Brandbestrijding",
            color = Color(0xFFF44336),
            icon = "🧯",
            meaning = "Locatie van brandblusser voor brandbestrijding.",
            examples = listOf("Gebouwen", "Parkeergarages", "Industriële ruimtes")
        ),
        SafetySign(
            id = "13",
            name = "Brandmelder",
            description = "Brandmelder",
            category = "Brandbestrijding",
            color = Color(0xFFF44336),
            icon = "🚨",
            meaning = "Brandmelder voor vroegtijdige waarschuwing bij brand.",
            examples = listOf("Gebouwen", "Hotels", "Kantoren")
        )
    )
    
    val filteredSigns = if (selectedCategory.value == "Alle") {
        safetySigns
    } else {
        safetySigns.filter { it.category == selectedCategory.value }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .padding(16.dp)
    ) {
        // Header
                Text(
            text = "Veiligheidsborden",
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
        
                Text(
            text = "Leer de betekenis van veiligheidsborden",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Category filter
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
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
        
        Spacer(modifier = Modifier.fillMaxWidth())
        
        // Dashboard button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val context = LocalContext.current
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
            // Sign icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(sign.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sign.icon,
                    fontSize = 24.sp
                )
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
            // Sign icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(sign.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sign.icon,
                    fontSize = 32.sp
                )
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