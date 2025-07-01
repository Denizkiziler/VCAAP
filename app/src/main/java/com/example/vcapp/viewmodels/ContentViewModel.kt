package com.example.vcapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vcapp.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContentViewModel : ViewModel() {
    private val repository = FirebaseContentRepository()
    
    // State flows
    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()
    
    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics: StateFlow<List<Topic>> = _topics.asStateFlow()
    
    private val _terms = MutableStateFlow<List<Term>>(emptyList())
    val terms: StateFlow<List<Term>> = _terms.asStateFlow()
    
    private val _signs = MutableStateFlow<List<Sign>>(emptyList())
    val signs: StateFlow<List<Sign>> = _signs.asStateFlow()
    
    private val _chemistryCards = MutableStateFlow<List<ChemistryCard>>(emptyList())
    val chemistryCards: StateFlow<List<ChemistryCard>> = _chemistryCards.asStateFlow()
    
    private val _exams = MutableStateFlow<List<Exam>>(emptyList())
    val exams: StateFlow<List<Exam>> = _exams.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Current chapter and topic
    private val _currentChapter = MutableStateFlow<Chapter?>(null)
    val currentChapter: StateFlow<Chapter?> = _currentChapter.asStateFlow()
    
    private val _currentTopic = MutableStateFlow<Topic?>(null)
    val currentTopic: StateFlow<Topic?> = _currentTopic.asStateFlow()
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load chapters
                val chaptersData = repository.getChapters()
                _chapters.value = chaptersData
                
                // Load terms
                val termsData = repository.getTerms()
                _terms.value = termsData
                
                // Load signs
                val signsData = repository.getSigns()
                _signs.value = signsData
                
                // Load chemistry cards
                val chemistryData = repository.getChemistryCards()
                _chemistryCards.value = chemistryData
                
                // Load exams
                val examsData = repository.getExams()
                _exams.value = examsData
                
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Fout bij het laden van content: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadTopicsForChapter(chapterId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val topicsData = repository.getTopicsForChapter(chapterId)
                _topics.value = topicsData
                
                // Set current chapter
                _currentChapter.value = _chapters.value.find { it.id == chapterId }
                
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Fout bij het laden van onderwerpen: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun setCurrentTopic(topicId: String) {
        _currentTopic.value = _topics.value.find { it.id == topicId }
    }
    
    fun saveChapterProgress(chapterId: String, progress: Float) {
        viewModelScope.launch {
            try {
                repository.saveChapterProgress(chapterId, progress)
                
                // Update local state
                _chapters.value = _chapters.value.map { chapter ->
                    if (chapter.id == chapterId) {
                        chapter.copy(progress = progress)
                    } else {
                        chapter
                    }
                }
            } catch (e: Exception) {
                _error.value = "Fout bij het opslaan van voortgang: ${e.message}"
            }
        }
    }
    
    fun saveTopicCompletion(topicId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                repository.saveTopicCompletion(topicId, isCompleted)
                
                // Update local state
                _topics.value = _topics.value.map { topic ->
                    if (topic.id == topicId) {
                        topic.copy(isCompleted = isCompleted)
                    } else {
                        topic
                    }
                }
                
                // Update current topic if it's the one being completed
                _currentTopic.value?.let { currentTopic ->
                    if (currentTopic.id == topicId) {
                        _currentTopic.value = currentTopic.copy(isCompleted = isCompleted)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Fout bij het opslaan van voltooiing: ${e.message}"
            }
        }
    }
    
    fun searchTerms(query: String): List<Term> {
        return if (query.isEmpty()) {
            _terms.value
        } else {
            _terms.value.filter { term ->
                term.term.contains(query, ignoreCase = true) ||
                term.definition.contains(query, ignoreCase = true)
            }
        }
    }
    
    fun getTermsByCategory(category: String): List<Term> {
        return if (category == "Alle") {
            _terms.value
        } else {
            _terms.value.filter { it.category == category }
        }
    }
    
    fun getCategories(): List<String> {
        return listOf("Alle") + _terms.value.map { it.category }.distinct().sorted()
    }
    
    fun getTotalProgress(): Float {
        val chapters = _chapters.value
        if (chapters.isEmpty()) return 0f
        
        val totalProgress = chapters.sumOf { it.progress.toDouble() }
        return (totalProgress / chapters.size).toFloat()
    }
    
    fun getCompletedTopicsCount(): Int {
        return _topics.value.count { it.isCompleted }
    }
    
    fun getTotalTopicsCount(): Int {
        return _topics.value.size
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun refreshData() {
        loadInitialData()
    }
} 