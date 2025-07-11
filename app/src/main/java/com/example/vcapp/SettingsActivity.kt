package com.example.vcapp

import android.content.Context
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
class SettingsActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        setContent {
            SettingsScreen(
                onLanguageChange = { newLang ->
                    saveAndApplyLanguage(newLang)
                },
                onPasswordChange = {
                    startActivity(Intent(this, PasswordResetActivity::class.java))
                },
                onSavePersonalInfo = { name, email ->
                    savePersonalInfo(name, email)
                },
                onResetStatistics = {
                    resetExamStatistics()
                }
            )
        }
    }

    private fun saveAndApplyLanguage(lang: String) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().putString("lang", lang).apply()
        
        try {
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            createConfigurationContext(config)
            recreate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun savePersonalInfo(name: String, email: String) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_name", name)
            .putString("user_email", email)
            .apply()
        
        Toast.makeText(this, "Personalia opgeslagen!", Toast.LENGTH_SHORT).show()
    }

    private fun resetExamStatistics() {
        val prefs = getSharedPreferences("exam_results", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        Toast.makeText(this, "Examenstatistieken gereset!", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLanguageChange: (String) -> Unit,
    onPasswordChange: () -> Unit,
    onSavePersonalInfo: (String, String) -> Unit,
    onResetStatistics: () -> Unit
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    val isDarkMode = remember { mutableStateOf(false) }
    val ttsVoice = remember { mutableStateOf("female") }
    
    // Load saved settings
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val savedLang = prefs.getString("lang", "nl") ?: "nl"
    val savedName = prefs.getString("user_name", "Jan Jansen") ?: "Jan Jansen"
    val savedEmail = prefs.getString("user_email", "jan@email.com") ?: "jan@email.com"
    
    val languages = listOf(
        "nl" to "Nederlands",
        "en" to "English",
        "de" to "Deutsch",
        "tr" to "Türkçe",
        "pl" to "Polski",
        "bg" to "Български",
        "cs" to "Čeština",
        "sk" to "Slovenčina"
    )
    val expandedLang = remember { mutableStateOf(false) }
    val selectedLang = remember { mutableStateOf(savedLang) }
    val expandedVoice = remember { mutableStateOf(false) }
    val name = remember { mutableStateOf(savedName) }
    val email = remember { mutableStateOf(savedEmail) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0E7FF))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Instellingen", fontWeight = FontWeight.Bold, fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))
        // Day/Night mode
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dag/Nacht-modus", modifier = Modifier.weight(1f))
            Switch(checked = isDarkMode.value, onCheckedChange = { isDarkMode.value = it })
        }
        // TTS voice
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TTS-stem", modifier = Modifier.weight(1f))
            ExposedDropdownMenuBox(
                expanded = expandedVoice.value,
                onExpandedChange = { expandedVoice.value = !expandedVoice.value }
            ) {
                OutlinedTextField(
                    value = if (ttsVoice.value == "female") "Vrouwelijk" else "Mannelijk",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Stem") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVoice.value) },
                    modifier = Modifier.menuAnchor().weight(1f)
                )
                DropdownMenu(
                    expanded = expandedVoice.value,
                    onDismissRequest = { expandedVoice.value = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Vrouwelijk") },
                        onClick = {
                            ttsVoice.value = "female"
                            expandedVoice.value = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mannelijk") },
                        onClick = {
                            ttsVoice.value = "male"
                            expandedVoice.value = false
                        }
                    )
                }
            }
        }
        // Language
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Taal", modifier = Modifier.weight(1f))
            ExposedDropdownMenuBox(
                expanded = expandedLang.value,
                onExpandedChange = { expandedLang.value = !expandedLang.value }
            ) {
                OutlinedTextField(
                    value = languages.find { it.first == selectedLang.value }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Taal") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLang.value) },
                    modifier = Modifier.menuAnchor().weight(1f)
                )
                DropdownMenu(
                    expanded = expandedLang.value,
                    onDismissRequest = { expandedLang.value = false }
                ) {
                    languages.forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedLang.value = code
                                expandedLang.value = false
                                onLanguageChange(code)
                            }
                        )
                    }
                }
            }
        }
        // Change password
        Button(
            onClick = { onPasswordChange() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Wachtwoord wijzigen")
        }
        // Edit personal info
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Personalia", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = name.value,
                    onValueChange = { name.value = it },
                    label = { Text("Naam") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = email.value,
                    onValueChange = { email.value = it },
                    label = { Text("E-mail") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onSavePersonalInfo(name.value, email.value) },
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Opslaan")
                }
            }
        }
        // Reset exam statistics
        Button(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
        ) {
            Text("Reset examenstatistieken", color = Color.White)
        }
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Bevestigen") },
                text = { Text("Weet je zeker dat je alle examenstatistieken wilt resetten?") },
                confirmButton = {
                    Button(onClick = {
                        onResetStatistics()
                        showResetDialog = false
                    }) { Text("Ja") }
                },
                dismissButton = {
                    Button(onClick = { showResetDialog = false }) { Text("Annuleren") }
                }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        // Dashboard knop altijd onderaan
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = {
                    context.startActivity(Intent(context, DashboardActivity::class.java))
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
} 