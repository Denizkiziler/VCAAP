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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.vcapp.ui.theme.VCAPPTheme

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VCAPPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardActivityScreen()
                }
            }
        }
    }
}

@Composable
fun DashboardActivityScreen() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Welkom bij VCAAPP!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 32.dp, bottom = 8.dp)
            )
            Text(
                text = "Leer veilig werken en behaal je VCA certificering",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )
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
        // Vaste voortgangsbalk onderaan
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Jouw Voortgang",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProgressItem("Theorie", "35%", MaterialTheme.colorScheme.onPrimary)
                    ProgressItem("Examens", "1/3", MaterialTheme.colorScheme.onPrimary)
                    ProgressItem("Begrippen", "12/15", MaterialTheme.colorScheme.onPrimary)
                }
            }
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
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun ProgressItem(label: String, value: String, textColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = textColor.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardActivityScreenPreview() {
    VCAPPTheme {
        DashboardActivityScreen()
    }
} 