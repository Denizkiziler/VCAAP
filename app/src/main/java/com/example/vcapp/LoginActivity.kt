package com.example.vcapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.os.ConfigurationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

@OptIn(ExperimentalMaterial3Api::class)
class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Check if user is already signed in
        if (auth.currentUser != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }
        
        // Restore saved language
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("lang", Locale.getDefault().language) ?: "nl"
        setLocale(lang)
        setContent {
            LoginScreen(
                onLogin = { email, password, onError, onSuccess ->
                    performLogin(email, password, onError, onSuccess)
                },
                onRegister = {
                    startActivity(Intent(this, RegisterActivity::class.java))
                },
                onForgotPassword = {
                    startActivity(Intent(this, PasswordResetActivity::class.java))
                },
                onLanguageChange = { newLang ->
                    prefs.edit().putString("lang", newLang).apply()
                    setLocale(newLang)
                    recreate()
                },
                selectedLanguage = lang
            )
        }
    }

    private fun performLogin(email: String, password: String, onError: (String) -> Unit, onSuccess: () -> Unit) {
        if (email.isEmpty() || password.isEmpty()) {
            onError("Vul alle velden in")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    onSuccess()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "E-mailadres niet gevonden"
                        is FirebaseAuthInvalidCredentialsException -> "Onjuist wachtwoord"
                        else -> "Inloggen mislukt: ${task.exception?.message ?: "Onbekende fout"}"
                    }
                    onError(errorMessage)
                }
            }
    }

    private fun setLocale(lang: String) {
        try {
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            createConfigurationContext(config)
        } catch (e: Exception) {
            // Fallback to default locale if there's an error
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLogin: (String, String, (String) -> Unit, () -> Unit) -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onLanguageChange: (String) -> Unit,
    selectedLanguage: String
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val error = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val languages = listOf(
        "nl" to "Nederlands",
        "en" to "English",
        "de" to "Deutsch",
        "tr" to "Türkçe",
        "pl" to "Polski",
        "bg" to "Български",
        "ro" to "Română",
        "cs" to "Čeština",
        "sk" to "Slovenčina",
        "es" to "Español",
        "it" to "Italiano",
        "ar" to "العربية"
    )
    val expanded = remember { mutableStateOf(false) }
    val selectedLang = remember { mutableStateOf(selectedLanguage) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Language dropdown
            ExposedDropdownMenuBox(
                expanded = expanded.value,
                onExpandedChange = { expanded.value = !expanded.value }
            ) {
                OutlinedTextField(
                    value = languages.find { it.first == selectedLang.value }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kies taal") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                DropdownMenu(
                    expanded = expanded.value,
                    onDismissRequest = { expanded.value = false }
                ) {
                    languages.forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                expanded.value = false
                                selectedLang.value = code
                                onLanguageChange(code)
                            }
                        )
                    }
                }
            }
            Text(
                text = "Inloggen",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it },
                label = { Text("E-mailadres") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.padding(bottom = 12.dp),
                enabled = !isLoading.value
            )
            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Wachtwoord") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.padding(bottom = 12.dp),
                enabled = !isLoading.value
            )
            if (error.value.isNotEmpty()) {
                Text(
                    text = error.value,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Button(
                onClick = {
                    isLoading.value = true
                    error.value = ""
                    onLogin(
                        email.value,
                        password.value,
                        { errorMessage ->
                            error.value = errorMessage
                            isLoading.value = false
                        },
                        {
                            isLoading.value = false
                        }
                    )
                },
                modifier = Modifier.padding(bottom = 8.dp),
                enabled = !isLoading.value
            ) {
                if (isLoading.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
                Text("Inloggen", fontSize = 18.sp)
            }
            Text(
                text = "Registreren",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onRegister() }
                    .padding(bottom = 8.dp)
            )
            Text(
                text = "Wachtwoord vergeten?",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onForgotPassword() }
            )
        }
    }
} 