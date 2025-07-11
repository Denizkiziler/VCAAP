package com.example.vcapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text

@Composable
fun StatItem(label: String, value: String, textColor: Color) {
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