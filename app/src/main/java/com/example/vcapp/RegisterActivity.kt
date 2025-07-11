package com.example.vcapp

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class RegisterActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        setContent {
            RegisterScreen(
                onRegister = { email, password, confirmPassword, onError, onSuccess ->
                    performRegistration(email, password, confirmPassword, onError, onSuccess)
                },
                onBackToLogin = {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            )
        }
    }

    private fun performRegistration(
        email: String, 
        password: String, 
        confirmPassword: String,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        // Validation
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            onError("Vul alle velden in")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            onError("Voer een geldig e-mailadres in")
            return
        }

        if (password.length < 6) {
            onError("Wachtwoord moet minimaal 6 karakters bevatten")
            return
        }

        if (password != confirmPassword) {
            onError("Wachtwoorden komen niet overeen")
            return
        }

        // Create user with Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    onSuccess()
                    Toast.makeText(this, "Account succesvol aangemaakt!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthWeakPasswordException -> "Wachtwoord is te zwak"
                        is FirebaseAuthInvalidCredentialsException -> "Ongeldig e-mailadres"
                        is FirebaseAuthUserCollisionException -> "E-mailadres is al in gebruik"
                        else -> "Registratie mislukt: ${task.exception?.message ?: "Onbekende fout"}"
                    }
                    onError(errorMessage)
                }
            }
    }
}

@Composable
fun RegisterScreen(
    onRegister: (String, String, String, (String) -> Unit, () -> Unit) -> Unit,
    onBackToLogin: () -> Unit
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val error = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Registreren",
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
            OutlinedTextField(
                value = confirmPassword.value,
                onValueChange = { confirmPassword.value = it },
                label = { Text("Bevestig wachtwoord") },
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
                    onRegister(
                        email.value,
                        password.value,
                        confirmPassword.value,
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
                Text("Account aanmaken", fontSize = 18.sp)
            }
            Text(
                text = "Terug naar inloggen",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onBackToLogin() }
            )
        }
    }
} 