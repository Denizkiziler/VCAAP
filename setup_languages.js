const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
// You need to download your service account key from Firebase Console
// and place it in the same directory as this script
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Languages data to add to Firestore
const languages = [
  {
    code: "en",
    name: "English",
    flag: "🇬🇧",
    order: 1,
    isActive: true
  },
  {
    code: "tr",
    name: "Türkçe",
    flag: "🇹🇷",
    order: 2,
    isActive: true
  },
  {
    code: "es",
    name: "Español",
    flag: "🇪🇸",
    order: 3,
    isActive: true
  },
  {
    code: "uk",
    name: "Українська",
    flag: "🇺🇦",
    order: 4,
    isActive: true
  },
  {
    code: "de",
    name: "Deutsch",
    flag: "🇩🇪",
    order: 5,
    isActive: true
  },
  {
    code: "fr",
    name: "Français",
    flag: "🇫🇷",
    order: 6,
    isActive: true
  },
  {
    code: "it",
    name: "Italiano",
    flag: "🇮🇹",
    order: 7,
    isActive: true
  },
  {
    code: "ar",
    name: "العربية",
    flag: "🇸🇦",
    order: 8,
    isActive: true
  },
  {
    code: "cs",
    name: "Čeština",
    flag: "🇨🇿",
    order: 9,
    isActive: true
  },
  {
    code: "ro",
    name: "Română",
    flag: "🇷🇴",
    order: 10,
    isActive: true
  },
  {
    code: "bg",
    name: "Български",
    flag: "🇧🇬",
    order: 11,
    isActive: true
  }
];

async function setupLanguages() {
  try {
    console.log('Starting to add languages to Firestore...');
    
    const batch = db.batch();
    
    languages.forEach((language, index) => {
      const docRef = db.collection('languages').doc(language.code);
      batch.set(docRef, language);
      console.log(`Added language: ${language.name} (${language.code})`);
    });
    
    await batch.commit();
    console.log('Successfully added all languages to Firestore!');
    
  } catch (error) {
    console.error('Error adding languages:', error);
  } finally {
    process.exit(0);
  }
}

setupLanguages(); 