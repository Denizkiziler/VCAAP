package com.example.vcapp.data

// Data classes for VCA content
data class Chapter(
    val id: String,
    val title: String,
    val description: String,
    val icon: Int,
    val topics: List<Topic>,
    val progress: Float = 0f
)

data class Topic(
    val id: String,
    val title: String,
    val content: String,
    val imageUrl: String? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val isCompleted: Boolean = false
)

data class QuizQuestion(
    val id: String,
    val question: String,
    val correctAnswer: String,
    val explanation: String? = null
)

data class Exam(
    val id: String,
    val title: String,
    val description: String,
    val questions: List<ExamQuestion>,
    val timeLimit: Int? = null, // in minutes
    val passingScore: Int = 70
)

data class ExamQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int, // index of correct option
    val explanation: String? = null,
    val imageUrl: String? = null
)

data class Term(
    val id: String,
    val term: String,
    val definition: String,
    val category: String,
    val imageUrl: String? = null,
    val uitleg: String? = null
)

data class Sign(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val category: String,
    val meaning: String
)

data class ChemistryCard(
    val id: String,
    val name: String,
    val symbol: String,
    val atomicNumber: Int,
    val category: String,
    val description: String,
    val imageUrl: String? = null
)

// Mock data for development
object MockContentData {
    val chapters = listOf(
        Chapter(
            id = "1",
            title = "Hoofdstuk 1: Veiligheid",
            description = "Basisprincipes van veiligheid op de werkvloer",
            icon = com.example.vcapp.R.drawable.ic_theorie,
            topics = listOf(
                Topic(
                    id = "1.1",
                    title = "Wat is VCA?",
                    content = "VCA staat voor Veiligheid Checklist Aannemers. Het is een certificeringssysteem dat aantoont dat een bedrijf veilig werkt en voldoet aan de wettelijke veiligheidseisen.\n\nBelangrijke punten:\n• VCA is verplicht voor veel opdrachtgevers\n• Het toont aan dat je veiligheidsbewust bent\n• Het verhoogt je kansen op opdrachten\n• Het voorkomt ongelukken en letsel",
                    questions = listOf(
                        QuizQuestion(
                            id = "q1.1.1",
                            question = "Waar staat VCA voor?",
                            correctAnswer = "Veiligheid Checklist Aannemers",
                            explanation = "VCA is een afkorting die staat voor Veiligheid Checklist Aannemers."
                        ),
                        QuizQuestion(
                            id = "q1.1.2",
                            question = "Waarom is VCA belangrijk?",
                            correctAnswer = "Het toont aan dat je veiligheidsbewust bent en voorkomt ongelukken",
                            explanation = "VCA certificering toont aan dat je bedrijf veilig werkt en voldoet aan wettelijke eisen."
                        )
                    )
                ),
                Topic(
                    id = "1.2",
                    title = "Persoonlijke Beschermingsmiddelen",
                    content = "Persoonlijke Beschermingsmiddelen (PBM) zijn essentiële hulpmiddelen om jezelf te beschermen tegen gevaren op de werkvloer.\n\nVeelgebruikte PBM:\n• Veiligheidshelm\n• Veiligheidsbril\n• Gehoorbescherming\n• Veiligheidschoenen\n• Werkhandschoenen\n• Ademhalingsbescherming",
                    questions = listOf(
                        QuizQuestion(
                            id = "q1.2.1",
                            question = "Wat is het doel van PBM?",
                            correctAnswer = "Bescherming tegen gevaren op de werkvloer",
                            explanation = "PBM beschermt de drager tegen specifieke gevaren die niet door andere maatregelen kunnen worden geëlimineerd."
                        )
                    )
                )
            )
        ),
        Chapter(
            id = "2",
            title = "Hoofdstuk 2: Gezondheid",
            description = "Gezondheidsrisico's en preventieve maatregelen",
            icon = com.example.vcapp.R.drawable.ic_theorie,
            topics = listOf(
                Topic(
                    id = "2.1",
                    title = "Gezondheidsrisico's",
                    content = "Op de werkvloer kunnen verschillende gezondheidsrisico's voorkomen. Het is belangrijk deze te herkennen en te weten hoe je jezelf kunt beschermen.\n\nVeelvoorkomende risico's:\n• Blootstelling aan gevaarlijke stoffen\n• Fysieke belasting\n• Lawaai\n• Trillingen\n• Stress",
                    questions = listOf(
                        QuizQuestion(
                            id = "q2.1.1",
                            question = "Welke gezondheidsrisico's kunnen voorkomen op de werkvloer?",
                            correctAnswer = "Blootstelling aan gevaarlijke stoffen, fysieke belasting, lawaai",
                            explanation = "Dit zijn de meest voorkomende gezondheidsrisico's op de werkvloer."
                        )
                    )
                )
            )
        ),
        Chapter(
            id = "3",
            title = "Hoofdstuk 3: Milieu",
            description = "Milieubewust werken en duurzaamheid",
            icon = com.example.vcapp.R.drawable.ic_theorie,
            topics = listOf(
                Topic(
                    id = "3.1",
                    title = "Milieubewust Werken",
                    content = "Milieubewust werken betekent dat je rekening houdt met de impact van je werkzaamheden op het milieu.\n\nBelangrijke principes:\n• Voorkom afval\n• Hergebruik materialen\n• Recycle waar mogelijk\n• Gebruik milieuvriendelijke producten\n• Bespaar energie",
                    questions = listOf(
                        QuizQuestion(
                            id = "q3.1.1",
                            question = "Wat betekent milieubewust werken?",
                            correctAnswer = "Rekening houden met de impact op het milieu",
                            explanation = "Milieubewust werken betekent bewust zijn van en rekening houden met de milieueffecten van je werkzaamheden."
                        )
                    )
                )
            )
        ),
        Chapter(
            id = "4",
            title = "Hoofdstuk 4: Gevaren",
            description = "Herkenning en preventie van gevaren",
            icon = com.example.vcapp.R.drawable.ic_theorie,
            topics = listOf(
                Topic(
                    id = "4.1",
                    title = "Gevarenherkenning",
                    content = "Het herkennen van gevaren is de eerste stap naar een veilige werkplek.\n\nSoorten gevaren:\n• Mechanische gevaren\n• Elektrische gevaren\n• Chemische gevaren\n• Biologische gevaren\n• Ergonomische gevaren",
                    questions = listOf(
                        QuizQuestion(
                            id = "q4.1.1",
                            question = "Wat is de eerste stap naar een veilige werkplek?",
                            correctAnswer = "Gevarenherkenning",
                            explanation = "Je kunt alleen maatregelen nemen tegen gevaren als je ze eerst herkent."
                        )
                    )
                )
            )
        ),
        Chapter(
            id = "5",
            title = "Hoofdstuk 5: PBM",
            description = "Persoonlijke Beschermingsmiddelen in detail",
            icon = com.example.vcapp.R.drawable.ic_theorie,
            topics = listOf(
                Topic(
                    id = "5.1",
                    title = "Selectie en Gebruik van PBM",
                    content = "De juiste selectie en het correcte gebruik van PBM is cruciaal voor effectieve bescherming.\n\nSelectiecriteria:\n• Type gevaar\n• Blootstellingsduur\n• Comfort en bruikbaarheid\n• Onderhoud en reiniging\n• Vervangingsfrequentie",
                    questions = listOf(
                        QuizQuestion(
                            id = "q5.1.1",
                            question = "Waarop moet je letten bij de selectie van PBM?",
                            correctAnswer = "Type gevaar, blootstellingsduur, comfort en bruikbaarheid",
                            explanation = "Deze factoren zijn essentieel voor het kiezen van de juiste PBM."
                        )
                    )
                )
            )
        ),
        Chapter(
            id = "6",
            title = "Hoofdstuk 6: Procedures",
            description = "Veiligheidsprocedures en werkwijzen",
            icon = com.example.vcapp.R.drawable.ic_theorie,
            topics = listOf(
                Topic(
                    id = "6.1",
                    title = "Veiligheidsprocedures",
                    content = "Veiligheidsprocedures zijn gestandaardiseerde werkwijzen die ervoor zorgen dat werkzaamheden veilig worden uitgevoerd.\n\nBelangrijke elementen:\n• Stap-voor-stap instructies\n• Verantwoordelijkheden\n• Noodprocedures\n• Controlelijsten\n• Training en instructie",
                    questions = listOf(
                        QuizQuestion(
                            id = "q6.1.1",
                            question = "Waarom zijn veiligheidsprocedures belangrijk?",
                            correctAnswer = "Ze zorgen voor gestandaardiseerde veilige werkwijzen",
                            explanation = "Procedures zorgen ervoor dat iedereen op dezelfde veilige manier werkt."
                        )
                    )
                )
            )
        )
    )

    val examQuestions = listOf(
        ExamQuestion(
            id = "exam1",
            question = "Waar staat VCA voor?",
            options = listOf(
                "Veiligheid Checklist Aannemers",
                "Veiligheid Controle Aannemers",
                "Veiligheid Certificering Aannemers",
                "Veiligheid Checklist Arbeid"
            ),
            correctAnswer = 0,
            explanation = "VCA staat voor Veiligheid Checklist Aannemers."
        ),
        ExamQuestion(
            id = "exam2",
            question = "Welke PBM is verplicht op de meeste bouwplaatsen?",
            options = listOf(
                "Veiligheidshelm",
                "Veiligheidsbril",
                "Gehoorbescherming",
                "Alle bovenstaande"
            ),
            correctAnswer = 3,
            explanation = "Afhankelijk van de werkzaamheden kunnen alle PBM verplicht zijn."
        ),
        ExamQuestion(
            id = "exam3",
            question = "Wat is de eerste stap bij het herkennen van gevaren?",
            options = listOf(
                "Direct ingrijpen",
                "Gevaren identificeren",
                "PBM aantrekken",
                "De leidinggevende waarschuwen"
            ),
            correctAnswer = 1,
            explanation = "Je moet eerst weten wat de gevaren zijn voordat je maatregelen kunt nemen."
        )
    )

    val terms = listOf(
        Term(
            id = "term1",
            term = "VCA",
            definition = "Veiligheid Checklist Aannemers - een certificeringssysteem voor veilig werken",
            category = "Algemeen"
        ),
        Term(
            id = "term2",
            term = "PBM",
            definition = "Persoonlijke Beschermingsmiddelen - hulpmiddelen om jezelf te beschermen",
            category = "Veiligheid"
        ),
        Term(
            id = "term3",
            term = "RI&E",
            definition = "Risico-Inventarisatie en -Evaluatie - verplichte analyse van arbeidsrisico's",
            category = "Wettelijk"
        )
    )

    val signs = listOf(
        Sign(
            id = "sign1",
            title = "Verplicht - Veiligheidshelm",
            description = "Verplicht om een veiligheidshelm te dragen",
            imageUrl = "",
            category = "Verplicht",
            meaning = "Je moet een veiligheidshelm dragen in dit gebied"
        ),
        Sign(
            id = "sign2",
            title = "Verbod - Roken",
            description = "Verboden om te roken",
            imageUrl = "",
            category = "Verbod",
            meaning = "Roken is niet toegestaan in dit gebied"
        ),
        Sign(
            id = "sign3",
            title = "Waarschuwing - Elektrische spanning",
            description = "Gevaar voor elektrische spanning",
            imageUrl = "",
            category = "Waarschuwing",
            meaning = "Pas op voor elektrische spanning"
        )
    )

    val chemistryCards = listOf(
        ChemistryCard(
            id = "chem1",
            name = "Waterstof",
            symbol = "H",
            atomicNumber = 1,
            category = "Niet-metaal",
            description = "Het lichtste en meest voorkomende element in het universum"
        ),
        ChemistryCard(
            id = "chem2",
            name = "Zuurstof",
            symbol = "O",
            atomicNumber = 8,
            category = "Niet-metaal",
            description = "Essentieel voor verbranding en ademhaling"
        ),
        ChemistryCard(
            id = "chem3",
            name = "Koolstof",
            symbol = "C",
            atomicNumber = 6,
            category = "Niet-metaal",
            description = "Basis voor alle organische verbindingen"
        )
    )
} 