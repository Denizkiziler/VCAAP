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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vcapp.viewmodels.ContentViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import com.google.firebase.firestore.FirebaseFirestore

class ChapterTopicsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chapterId = intent.getStringExtra("chapter_id") ?: ""
        val chapterTitle = intent.getStringExtra("chapter_title") ?: "Hoofdstuk"
        setContent {
            ChapterTopicsScreen(chapterId = chapterId, chapterTitle = chapterTitle, onTopicClick = { topic, topicTitle, hasSubtopics ->
                val intent = Intent(this, TheorieContentActivity::class.java)
                intent.putExtra("chapter_id", chapterId)
                intent.putExtra("topic_slug", topic)
                intent.putExtra("topic_title", topicTitle)
                intent.putExtra("has_subtopics", hasSubtopics)
                startActivity(intent)
            })
        }
    }
}

@Composable
fun ChapterTopicsScreen(chapterId: String, chapterTitle: String, onTopicClick: (String, String, Boolean) -> Unit) {
    val topics = remember { mutableStateOf<List<Triple<String, String, Boolean>>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    LaunchedEffect(chapterId) {
        val db = FirebaseFirestore.getInstance()
        db.collection("theorieHoofdstukken").document(chapterId).get().addOnSuccessListener { doc ->
            val onderwerpen = (doc["onderwerpen"] as? List<*>)?.mapNotNull { it as? Map<String, *> } ?: emptyList()
            topics.value = onderwerpen.map {
                val slug = it["slug"] as? String ?: ""
                val title = slug.replaceFirstChar { c -> c.uppercase() }
                val hasSubtopics = (it["subonderwerpen"] as? List<*>)?.isNotEmpty() == true
                Triple(slug, title, hasSubtopics)
            }
            isLoading.value = false
        }.addOnFailureListener { isLoading.value = false }
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(chapterTitle, fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))
        if (isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(topics.value) { triple ->
                    val slug = triple.first
                    val title = triple.second
                    val hasSubtopics = triple.third
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTopicClick(slug, title, hasSubtopics) }
                            .padding(4.dp)
                    ) {
                        Box(modifier = Modifier.padding(20.dp)) {
                            Text(title, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopicGrid(topics: List<com.example.vcapp.data.Topic>, chapterId: String) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        topics.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { topic ->
                    TopicCard(
                        topic = topic,
                        onClick = {
                            val intent = Intent(context, TheorieContentActivity::class.java)
                            intent.putExtra("chapter_id", chapterId)
                            intent.putExtra("topic_id", topic.id)
                            context.startActivity(intent)
                        }
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun TopicCard(
    topic: com.example.vcapp.data.Topic,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                painter = painterResource(id = R.drawable.ic_theorie),
                contentDescription = topic.title,
                tint = Color(0xFF4B3DFE),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = topic.title,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (topic.isCompleted) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓ Voltooid",
                    color = Color.Green,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ChapterTopicsScreenPreview() {
    ChapterTopicsScreen(chapterId = "1", chapterTitle = "Voorbeeld Hoofdstuk") { _, _, _ -> }
} 