package com.example.vcapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

class TheorieActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TheorieOverviewScreen(onChapterClick = { chapterId, chapterTitle ->
                val intent = Intent(this, ChapterTopicsActivity::class.java)
                intent.putExtra("chapter_id", chapterId)
                intent.putExtra("chapter_title", chapterTitle)
                startActivity(intent)
            })
        }
    }
}

@Composable
fun TheorieOverviewScreen(onChapterClick: (String, String) -> Unit) {
    var chapters by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("theorieHoofdstukken").get().addOnSuccessListener { snapshot ->
            chapters = snapshot.documents.mapNotNull { doc ->
                val title = doc.getString("title") ?: doc.id
                doc.id to title
            }
            isLoading = false
        }.addOnFailureListener { isLoading = false }
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Theorieoverzicht", fontSize = 28.sp, modifier = Modifier.padding(bottom = 16.dp))
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(chapters) { (id, title) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChapterClick(id, title) }
                            .padding(4.dp)
                    ) {
                        Box(modifier = Modifier.padding(20.dp)) {
                            Text(title, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
} 