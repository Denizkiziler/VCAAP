package com.example.vcapp

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TTSHelper(context: Context, lang: String = Locale.getDefault().language) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var language = lang
    private var context = context
    private val TAG = "TTSHelper"

    init {
        Log.d(TAG, "Initializing TTSHelper with language: $language")
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        Log.d(TAG, "TTS onInit called with status: $status")
        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "TTS initialization successful")
            val locale = when (language) {
                "nl" -> Locale("nl")
                "en" -> Locale.ENGLISH
                "de" -> Locale.GERMAN
                "tr" -> Locale("tr")
                "pl" -> Locale("pl")
                "bg" -> Locale("bg")
                "cs" -> Locale("cs")
                "sk" -> Locale("sk")
                else -> Locale.getDefault()
            }
            Log.d(TAG, "Setting TTS language to: $locale")
            val result = tts?.setLanguage(locale)
            ready = result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE
            Log.d(TAG, "Language set result: $result, ready: $ready")
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
            ready = false
        }
    }

    fun speak(text: String) {
        Log.d(TAG, "Speak called with text: ${text.take(50)}... (length: ${text.length})")
        Log.d(TAG, "TTS ready: $ready, tts instance: ${tts != null}")
        
        if (ready && tts != null) {
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            Log.d(TAG, "TTS speak result: $result")
        } else {
            Log.w(TAG, "TTS not ready or null. Ready: $ready, TTS: ${tts != null}")
        }
    }

    fun isReady(): Boolean {
        return ready
    }

    fun shutdown() {
        Log.d(TAG, "Shutting down TTS")
        tts?.shutdown()
        tts = null
        ready = false
    }
} 