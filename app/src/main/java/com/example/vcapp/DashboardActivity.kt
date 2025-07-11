package com.example.vcapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Logout
import com.example.vcapp.ui.theme.VCAPPTheme
import com.example.vcapp.ui.StatItem
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Check if user is signed in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        setContent {
            VCAPPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardActivityScreen(
                        onLogout = {
                            performLogout()
                        }
                    )
                }
            }
        }
    }

    private fun performLogout() {
        auth.signOut()
        Toast.makeText(this, "Uitgelogd", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

@Composable
fun DashboardActivityScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E7FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with gradient background
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welkom bij VCAAPP!",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Leer veilig werken en behaal je VCA certificering",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            // Stats row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                StatItem("Theorie", "35%", Color.White)
                                StatItem("Examens", "1/3", Color.White)
                                StatItem("Begrippen", "12/15", Color.White)
                            }
                        }
                        
                        IconButton(
                            onClick = { onLogout() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Logout,
                                contentDescription = "Uitloggen",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            DashboardCard(
                title = "Theorie",
                description = "Leer de basisprincipes",
                icon = R.drawable.ic_theorie,
                onClick = { context.startActivity(Intent(context, TheorieActivity::class.java)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            DashboardCard(
                title = "Examen",
                description = "Test je kennis",
                icon = R.drawable.ic_examen,
                onClick = { context.startActivity(Intent(context, ExamOverviewActivity::class.java)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            DashboardCard(
                title = "Borden",
                description = "Veiligheidsborden",
                icon = R.drawable.ic_borden,
                onClick = { context.startActivity(Intent(context, BordenActivity::class.java)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            DashboardCard(
                title = "Begrippen",
                description = "VCA termen",
                icon = R.drawable.ic_begrippen,
                onClick = { context.startActivity(Intent(context, BegrippenActivity::class.java)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            DashboardCard(
                title = "Chemiekaart",
                description = "Periodiek systeem",
                icon = R.drawable.ic_chemiekaart,
                onClick = { context.startActivity(Intent(context, ChemiekaartActivity::class.java)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            DashboardCard(
                title = "Instellingen",
                description = "App configuratie",
                icon = R.drawable.ic_launcher_foreground,
                onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    description: String,
    icon: Int,
    onClick: () -> Unit
) {
    val cardColors = listOf(
        Color(0xFF4F46E5), // Indigo
        Color(0xFF7C3AED), // Purple
        Color(0xFF059669), // Emerald
        Color(0xFFDC2626), // Red
        Color(0xFFEA580C), // Orange
        Color(0xFF2563EB)  // Blue
    )
    val cardColor = cardColors[title.hashCode() % cardColors.size]
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container with background
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
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



@Preview(showBackground = true)
@Composable
fun DashboardActivityScreenPreview() {
    VCAPPTheme {
        DashboardActivityScreen(onLogout = {})
    }
} 