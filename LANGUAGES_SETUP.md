# Talen Setup voor VCA App

## Overzicht
De app laadt nu dynamisch beschikbare talen uit Firestore in plaats van hardcoded talen. Dit maakt het mogelijk om talen toe te voegen/verwijderen zonder de app te updaten.

## Firestore Structuur

### Collectie: `languages`
Elk document in de `languages` collectie heeft de volgende structuur:

```json
{
  "code": "en",
  "name": "English", 
  "flag": "🇬🇧",
  "order": 1,
  "isActive": true
}
```

**Velden:**
- `code`: Taalcode (ISO 639-1, bijv. "en", "tr", "es")
- `name`: Naam van de taal in de eigen taal
- `flag`: Emoji vlag voor de taal
- `order`: Volgorde waarin talen worden getoond
- `isActive`: Of de taal beschikbaar is in de app

## Automatische Setup

### Optie 1: Node.js Script (Aanbevolen)
1. Download je Firebase service account key van de Firebase Console
2. Plaats het bestand als `serviceAccountKey.json` in de root van het project
3. Installeer dependencies: `npm install firebase-admin`
4. Voer het script uit: `node setup_languages.js`

### Optie 2: Handmatig via Firebase Console
1. Ga naar [Firebase Console](https://console.firebase.google.com/)
2. Open je project en ga naar Firestore Database
3. Maak een nieuwe collectie genaamd `languages`
4. Voeg voor elke taal een document toe met de taalcode als document ID

## Beschikbare Talen

De volgende talen zijn geconfigureerd:

| Code | Naam | Vlag | Order |
|------|------|------|-------|
| en | English | 🇬🇧 | 1 |
| tr | Türkçe | 🇹🇷 | 2 |
| es | Español | 🇪🇸 | 3 |
| uk | Українська | 🇺🇦 | 4 |
| de | Deutsch | 🇩🇪 | 5 |
| fr | Français | 🇫🇷 | 6 |
| it | Italiano | 🇮🇹 | 7 |
| ar | العربية | 🇸🇦 | 8 |
| cs | Čeština | 🇨🇿 | 9 |
| ro | Română | 🇷🇴 | 10 |
| bg | Български | 🇧🇬 | 11 |

## Nieuwe Talen Toevoegen

1. Voeg een nieuw document toe aan de `languages` collectie
2. Gebruik de taalcode als document ID
3. Vul alle vereiste velden in
4. Zet `isActive` op `true` om de taal beschikbaar te maken

## Talen Uitschakelen

Zet `isActive` op `false` om een taal tijdelijk uit te schakelen zonder het document te verwijderen.

## App Functionaliteit

- **LoginActivity**: Toont taalkeuze dropdown met talen uit Firestore
- **SettingsActivity**: Toont taalkeuze dropdown onderaan het scherm
- **Fallback**: Als Firestore niet beschikbaar is, gebruikt de app de hardcoded lijst
- **Caching**: Talen worden opgeslagen in SharedPreferences

## Technische Details

- **Repository**: `FirebaseContentRepository.getAvailableLanguages()`
- **ViewModel**: `ContentViewModel.loadLanguages()`
- **Data Class**: `Language` in `ContentData.kt`
- **Error Handling**: Fallback naar hardcoded lijst bij netwerkproblemen 