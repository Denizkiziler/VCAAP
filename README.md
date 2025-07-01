# VCA Learning App

Een uitgebreide Android leerapp voor VCA (Veiligheid, gezondheid en milieu Checklist Aannemers) examenvoorbereiding met interactieve functies, theorie, examens en voortgangs tracking.

## Functies

### 📚 Theorie Sectie
- **Uitgebreide Theorie**: Complete VCA theorie met hoofdstukken en onderwerpen
- **Interactieve Content**: Navigeer door verschillende theorie onderdelen
- **TTS Ondersteuning**: Text-to-Speech voor toegankelijkheid

### 🎯 Examen Systeem
- **VCA Basis**: 40 vragen in 60 minuten
- **VCA VOL**: 70 vragen in 105 minuten
- **Realistische Examens**: Officiële VCA examenvorm
- **Tijdslimiet**: Echte examentijd simulatie

### 📊 Voortgangs Tracking
- **Examen Resultaten**: Bewaar en bekijk je scores
- **Overgeslagen Vragen**: Track welke vragen je hebt overgeslagen
- **Statistieken**: Analyseer je prestaties

### 🔧 Instellingen
- **Taal Selectie**: Ondersteuning voor meerdere talen
- **TTS Instellingen**: Kies tussen mannelijke en vrouwelijke stem
- **Dark Mode**: Dag/nacht modus
- **Persoonlijke Gegevens**: Bewaar je profiel informatie

## Technische Stack

- **UI Framework**: Jetpack Compose
- **Architectuur**: MVVM met ViewModel
- **State Management**: Kotlin Flow
- **Taal**: Kotlin
- **TTS**: Android TextToSpeech

## Setup Instructies

### Vereisten
- Android Studio Arctic Fox of later
- Android SDK 24+

### Installatie

1. **Clone de repository**
   ```bash
   git clone <repository-url>
   cd VCAAPP-app
   ```

2. **Build en Run**
   ```bash
   ./gradlew build
   ```

## App Structuur

```
app/src/main/java/com/example/vcapp/
├── MainActivity.kt              # Hoofdactiviteit met Compose setup
├── LoginActivity.kt            # Login scherm
├── RegisterActivity.kt         # Registratie scherm
├── PasswordResetActivity.kt    # Wachtwoord reset
├── ChapterTopicsActivity.kt    # Theorie hoofdstukken
├── TheorieActivity.kt          # Theorie navigatie
├── TheorieContentActivity.kt   # Theorie content weergave
├── ExamOverviewActivity.kt     # Examen overzicht
├── ExamQuestionActivity.kt     # Examen vragen
├── SettingsActivity.kt         # Instellingen
├── TTSHelper.kt               # Text-to-Speech helper
└── ui/theme/                   # Material 3 theming
```

## Belangrijke Componenten

### Examen Systeem
- **Vraag Bank**: Uitgebreide database van VCA vragen
- **Tijdslimiet**: Realistische examentijd simulatie
- **Score Tracking**: Bewaar en analyseer resultaten
- **Overslaan Functie**: Markeer vragen voor later

### Theorie Systeem
- **Hoofdstukken**: Georganiseerde theorie structuur
- **Onderwerpen**: Gedetailleerde content per hoofdstuk
- **TTS Ondersteuning**: Voorlees functionaliteit
- **Navigatie**: Eenvoudige navigatie door content

### Instellingen
- **Taal Ondersteuning**: Meerdere talen beschikbaar
- **TTS Configuratie**: Stem en snelheid instellingen
- **Dark Mode**: Thema ondersteuning
- **Profiel Beheer**: Persoonlijke gegevens

## VCA Content

De app bevat uitgebreide VCA educatie:

### VCA Basis
- Veiligheidsrisico's herkennen
- Basis veiligheidsregels
- Persoonlijke beschermingsmiddelen
- Gevaarlijke stoffen

### VCA VOL
- Leidinggevende verantwoordelijkheden
- Risico inventarisatie en evaluatie
- Werkvergunningen
- Incidenten en ongevallen

## Toekomstige Verbeteringen

### Geplande Functies
- [ ] **Offline Modus**: Download content voor offline gebruik
- [ ] **Cloud Sync**: Synchroniseer voortgang tussen apparaten
- [ ] **Analytics**: Track leerpatronen
- [ ] **Achievements**: Gamification met badges en beloningen
- [ ] **Video Tutorials**: Stap-voor-stap video's
- [ ] **Sociale Functies**: Deel voortgang en resultaten

### Technische Verbeteringen
- [ ] **Data Persistence**: Sla voortgang lokaal op
- [ ] **Database Integratie**: SQLite of Room database
- [ ] **API Integratie**: Haal content op van externe bronnen
- [ ] **Testing**: Voeg uitgebreide unit en UI tests toe
- [ ] **Accessibility**: Verbeter toegankelijkheidsfuncties

## Bijdragen

1. Fork de repository
2. Maak een feature branch
3. Maak je wijzigingen
4. Voeg tests toe indien van toepassing
5. Dien een pull request in

## Licentie

Dit project is gelicenseerd onder de MIT License - zie het LICENSE bestand voor details.

## Ondersteuning

Voor ondersteuning of vragen, open een issue in de repository of neem contact op met het development team.

---

**Succes met je VCA examen! 🎓** 