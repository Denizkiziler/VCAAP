package com.example.vcapp

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TTSHelper(context: Context, lang: String = Locale.getDefault().language) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var language = lang
    private var context = context

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
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
            val result = tts?.setLanguage(locale)
            ready = result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE
        }
    }

    fun speak(text: String) {
        if (ready) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.shutdown()
    }
} 