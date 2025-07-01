package com.example.vcapp.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FirebaseContentRepository {
    private val db = FirebaseFirestore.getInstance()
    
    // Collections
    private val chaptersCollection = db.collection("theorieHoofdstukken")
    private val topicsCollection = db.collection("topics")
    private val termsCollection = db.collection("afkortingen")
    private val signsCollection = db.collection("signs")
    private val chemistryCollection = db.collection("chemistry")
    private val examsCollection = db.collection("examenVragen")
    private val examQuestionsCollection = db.collection("exam_questions")
    
    // Get all chapters with topics
    suspend fun getChapters(): List<Chapter> {
        return try {
            val chaptersSnapshot = chaptersCollection
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()
            
            chaptersSnapshot.documents.mapNotNull { doc ->
                val chapterData = doc.data
                if (chapterData != null) {
                    Chapter(
                        id = doc.id,
                        title = chapterData["title"] as? String ?: "",
                        description = chapterData["description"] as? String ?: "",
                        icon = getIconResource(chapterData["icon"] as? String),
                        topics = emptyList(), // Will be loaded separately
                        progress = (chapterData["progress"] as? Number)?.toFloat() ?: 0f
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to mock data
            MockContentData.chapters
        }
    }
    
    // Get topics for a specific chapter
    suspend fun getTopicsForChapter(chapterId: String): List<Topic> {
        return try {
            val chapterDoc = chaptersCollection.document(chapterId)
            val onderwerpenSnapshot = chapterDoc.collection("onderwerpen")
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .await()
            onderwerpenSnapshot.documents.mapNotNull { onderwerpDoc ->
                val onderwerpData = onderwerpDoc.data
                if (onderwerpData != null) {
                    // Subonderwerpen ophalen uit array-veld
                    val subonderwerpenArray = onderwerpData["subonderwerpen"] as? List<*>
                    val subonderwerpen = subonderwerpenArray?.mapNotNull { subObj ->
                        (subObj as? Map<*, *>)?.let { subMap ->
                            val content = subMap["content"] as? String ?: ""
                            val imageHint = subMap["imageHint"] as? String
                            val imageSrc = subMap["imageSrc"] as? String
                            val questionMap = subMap["question"] as? Map<*, *>
                            val answersList = (questionMap?.get("answers") as? List<*>)?.mapNotNull { ans ->
                                (ans as? Map<*, *>)?.let { aMap ->
                                    QuizQuestion(
                                        id = (aMap["text"] as? String ?: "") + "_q",
                                        question = questionMap["text"] as? String ?: "",
                                        correctAnswer = aMap["text"] as? String ?: "",
                                        explanation = null
                                    )
                                }
                            } ?: emptyList()
                            Topic(
                                id = (subMap["title"] as? String ?: "") + "_sub",
                                title = subMap["title"] as? String ?: "",
                                content = content,
                                imageUrl = imageSrc,
                                questions = answersList,
                                isCompleted = false
                            )
                        }
                    } ?: emptyList()
                    // Hoofdonderwerp als Topic met subonderwerpen als content (optioneel)
                    Topic(
                        id = onderwerpDoc.id,
                        title = onderwerpData["title"] as? String ?: "",
                        content = subonderwerpen.joinToString("\n\n") { it.content },
                        imageUrl = null,
                        questions = subonderwerpen.flatMap { it.questions },
                        isCompleted = false
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Get quiz questions for a topic
    private suspend fun getQuizQuestionsForTopic(topicId: String): List<QuizQuestion> {
        return try {
            val questionsSnapshot = db.collection("quiz_questions")
                .whereEqualTo("topicId", topicId)
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()
            
            questionsSnapshot.documents.mapNotNull { doc ->
                val questionData = doc.data
                if (questionData != null) {
                    QuizQuestion(
                        id = doc.id,
                        question = questionData["question"] as? String ?: "",
                        correctAnswer = questionData["correctAnswer"] as? String ?: "",
                        explanation = questionData["explanation"] as? String
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Get all terms
    suspend fun getTerms(): List<Term> {
        return try {
            val termsSnapshot = termsCollection
                .orderBy("term", Query.Direction.ASCENDING)
                .get()
                .await()
            
            termsSnapshot.documents.mapNotNull { doc ->
                val termData = doc.data
                if (termData != null) {
                    Term(
                        id = doc.id,
                        term = termData["term"] as? String ?: "",
                        definition = termData["definitie"] as? String ?: "",
                        category = termData["category"] as? String ?: "",
                        imageUrl = termData["imageUrl"] as? String,
                        uitleg = termData["uitleg"] as? String
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to mock data
            MockContentData.terms.sortedBy { it.term }
        }
    }
    
    // Get all signs
    suspend fun getSigns(): List<Sign> {
        return try {
            val signsSnapshot = signsCollection
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .await()
            
            signsSnapshot.documents.mapNotNull { doc ->
                val signData = doc.data
                if (signData != null) {
                    Sign(
                        id = doc.id,
                        title = signData["title"] as? String ?: "",
                        description = signData["description"] as? String ?: "",
                        imageUrl = signData["imageUrl"] as? String ?: "",
                        category = signData["category"] as? String ?: "",
                        meaning = signData["meaning"] as? String ?: ""
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to mock data
            MockContentData.signs
        }
    }
    
    // Get all chemistry cards
    suspend fun getChemistryCards(): List<ChemistryCard> {
        return try {
            val chemistrySnapshot = chemistryCollection
                .orderBy("atomicNumber", Query.Direction.ASCENDING)
                .get()
                .await()
            
            chemistrySnapshot.documents.mapNotNull { doc ->
                val chemistryData = doc.data
                if (chemistryData != null) {
                    ChemistryCard(
                        id = doc.id,
                        name = chemistryData["name"] as? String ?: "",
                        symbol = chemistryData["symbol"] as? String ?: "",
                        atomicNumber = (chemistryData["atomicNumber"] as? Number)?.toInt() ?: 0,
                        category = chemistryData["category"] as? String ?: "",
                        description = chemistryData["description"] as? String ?: "",
                        imageUrl = chemistryData["imageUrl"] as? String
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to mock data
            MockContentData.chemistryCards
        }
    }
    
    // Get all exams
    suspend fun getExams(): List<Exam> {
        return try {
            val examsSnapshot = examsCollection
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()
            examsSnapshot.documents.mapNotNull { doc ->
                val examData = doc.data
                if (examData != null) {
                    val examTitle = examData["title"] as? String ?: ""
                    val isVolExam = examTitle.contains("VOL", ignoreCase = true)
                    
                    Exam(
                        id = doc.id,
                        title = examTitle,
                        description = examData["description"] as? String ?: "",
                        questions = getExamQuestionsForExam(doc.id).shuffled(),
                        // Hardcoded values: VCA Basis = 60 min, VCA VOL = 105 min
                        timeLimit = if (isVolExam) 105 else 60,
                        // Hardcoded passing scores: VCA Basis = 28/40 (70%), VCA VOL = 49/70 (70%)
                        passingScore = if (isVolExam) 49 else 28
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Return mock exam data with hardcoded values
            listOf(
                Exam(
                    id = "1",
                    title = "VCA Basis Examen",
                    description = "Basis VCA certificering voor alle medewerkers",
                    questions = MockContentData.examQuestions.shuffled(),
                    timeLimit = 60,
                    passingScore = 28
                )
            )
        }
    }
    
    // Get exam questions for a specific exam
    private suspend fun getExamQuestionsForExam(examId: String): List<ExamQuestion> {
        return try {
            val questionsSnapshot = examQuestionsCollection
                .whereEqualTo("examId", examId)
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()
            questionsSnapshot.documents.mapNotNull { doc ->
                val questionData = doc.data
                if (questionData != null) {
                    val originalOptions = (questionData["options"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val originalCorrectIndex = (questionData["correctAnswer"] as? Number)?.toInt() ?: 0
                    val correctAnswerText = originalOptions.getOrNull(originalCorrectIndex) ?: ""
                    val shuffledOptions = originalOptions.shuffled()
                    val newCorrectIndex = shuffledOptions.indexOf(correctAnswerText)
                    ExamQuestion(
                        id = doc.id,
                        question = questionData["question"] as? String ?: "",
                        options = shuffledOptions,
                        correctAnswer = newCorrectIndex,
                        explanation = questionData["explanation"] as? String,
                        imageUrl = questionData["imageUrl"] as? String
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: shuffle mockdata en update correctAnswer index
            MockContentData.examQuestions.map { q ->
                val shuffled = q.options.shuffled()
                val correctText = q.options[q.correctAnswer]
                val newIndex = shuffled.indexOf(correctText)
                q.copy(options = shuffled, correctAnswer = newIndex)
            }
        }
    }
    
    // Save user progress
    suspend fun saveChapterProgress(chapterId: String, progress: Float) {
        try {
            chaptersCollection.document(chapterId)
                .update("progress", progress)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Save topic completion
    suspend fun saveTopicCompletion(topicId: String, isCompleted: Boolean) {
        try {
            topicsCollection.document(topicId)
                .update("isCompleted", isCompleted)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Search terms
    suspend fun searchTerms(query: String): List<Term> {
        return try {
            val terms = getTerms()
            terms.filter { term ->
                term.term.contains(query, ignoreCase = true) ||
                term.definition.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Helper function to get icon resource
    private fun getIconResource(iconName: String?): Int {
        return when (iconName) {
            "ic_theorie" -> com.example.vcapp.R.drawable.ic_theorie
            "ic_examen" -> com.example.vcapp.R.drawable.ic_examen
            "ic_borden" -> com.example.vcapp.R.drawable.ic_borden
            "ic_begrippen" -> com.example.vcapp.R.drawable.ic_begrippen
            "ic_chemiekaart" -> com.example.vcapp.R.drawable.ic_chemiekaart
            else -> com.example.vcapp.R.drawable.ic_theorie
        }
    }
    
    // Flow versions for reactive updates
    fun getChaptersFlow(): Flow<List<Chapter>> = flow {
        emit(getChapters())
    }
    
    fun getTermsFlow(): Flow<List<Term>> = flow {
        emit(getTerms())
    }
    
    fun getSignsFlow(): Flow<List<Sign>> = flow {
        emit(getSigns())
    }
    
    fun getChemistryCardsFlow(): Flow<List<ChemistryCard>> = flow {
        emit(getChemistryCards())
    }
    
    fun getExamsFlow(): Flow<List<Exam>> = flow {
        emit(getExams())
    }
} 