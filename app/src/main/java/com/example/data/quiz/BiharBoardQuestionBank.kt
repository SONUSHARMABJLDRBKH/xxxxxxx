package com.example.data.quiz

import com.example.data.model.QuizQuestion
import org.json.JSONArray
import org.json.JSONObject

object BiharBoardQuestionBank {
    const val TARGET_QUESTIONS_PER_SUBJECT = 1000

    val allSubjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "Hindi", "English")

    fun subjectsForClass(selectedClass: Int): List<String> = when (selectedClass) {
        9, 10 -> listOf("Mathematics", "Physics", "Chemistry", "Biology", "Hindi", "English")
        else -> listOf("Mathematics", "Physics", "Chemistry", "Biology", "Hindi", "English")
    }

    /**
     * Parses a verified Bihar Board question dataset JSON array imported by an Admin.
     * Supports bulk import up to 1,000+ questions per subject without fabricating exam years.
     */
    fun parseImportedJson(jsonText: String, adminUid: String): Result<List<QuizQuestion>> {
        return try {
            val array = JSONArray(jsonText.trim())
            val list = mutableListOf<QuizQuestion>()
            for (i in 0 until array.length()) {
                val obj: JSONObject = array.getJSONObject(i)
                val qClass = obj.optInt("questionClass", obj.optInt("class", 10)).coerceIn(9, 12)
                val subject = obj.optString("subject", "Mathematics").trim()
                val chapter = obj.optString("chapter", "General").trim()
                val questionText = obj.optString("questionText", obj.optString("question", "")).trim()
                val optionA = obj.optString("optionA", "").trim()
                val optionB = obj.optString("optionB", "").trim()
                val optionC = obj.optString("optionC", "").trim()
                val optionD = obj.optString("optionD", "").trim()
                val correct = obj.optString("correctOption", "A").trim().uppercase().take(1)
                val explanation = obj.optString("explanation", "Bihar Board syllabus concept.").trim()
                val source = obj.optString("source", "Bihar Board Verified Import").trim()
                val examYear = obj.optString("examYear", "").trim()
                val customId = obj.optString("id", "bseb_imp_${System.currentTimeMillis()}_$i")
                if (questionText.isNotBlank() && optionA.isNotBlank() && optionB.isNotBlank() &&
                    optionC.isNotBlank() && optionD.isNotBlank() && correct in listOf("A", "B", "C", "D")
                ) {
                    list.add(
                        QuizQuestion(
                            id = customId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_"),
                            questionClass = qClass,
                            subject = subject,
                            chapter = chapter,
                            questionText = questionText,
                            optionA = optionA,
                            optionB = optionB,
                            optionC = optionC,
                            optionD = optionD,
                            correctOption = correct,
                            explanation = explanation,
                            source = source,
                            examYear = examYear,
                            createdBy = adminUid
                        )
                    )
                }
            }
            if (list.isEmpty()) Result.failure(IllegalArgumentException("No valid questions found in JSON"))
            else Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Starter verified Bihar Board syllabus question bank across Classes 9, 10, 11, and 12
     * and all 6 subjects (Mathematics, Physics, Chemistry, Biology, Hindi, English).
     * Admins can expand any subject up to 1,000+ questions via JSON dataset import.
     */
    fun starterQuestions(): List<QuizQuestion> = listOf(
        // --- CLASS 10 MATHEMATICS ---
        QuizQuestion(
            id = "bseb_10_math_001",
            questionClass = 10,
            subject = "Mathematics",
            chapter = "Trigonometry",
            questionText = "sin²θ + cos²θ का मान क्या होता है?",
            optionA = "0",
            optionB = "1",
            optionC = "2",
            optionD = "10",
            correctOption = "B",
            explanation = "यह त्रिकोणमिति की मूल सर्वसमिका (Fundamental Identity) है: sin²θ + cos²θ = 1।",
            source = "BSEB Class 10 Mathematics Ch. 8"
        ),
        QuizQuestion(
            id = "bseb_10_math_002",
            questionClass = 10,
            subject = "Mathematics",
            chapter = "Real Numbers",
            questionText = "दो परिमेय संख्याओं के बीच अधिकतम कितनी परिमेय संख्याएँ हो सकती हैं?",
            optionA = "1",
            optionB = "2",
            optionC = "3",
            optionD = "अनंत (Infinite)",
            correctOption = "D",
            explanation = "किन्हीं दो परिमेय संख्याओं के बीच अनंत परिमेय संख्याएँ होती हैं।",
            source = "BSEB Class 10 Mathematics Ch. 1"
        ),
        QuizQuestion(
            id = "bseb_10_math_003",
            questionClass = 10,
            subject = "Mathematics",
            chapter = "Quadratic Equations",
            questionText = "द्विघात समीकरण ax² + bx + c = 0 का विविक्तकर (Discriminant) D बराबर होता है:",
            optionA = "b² - 4ac",
            optionB = "b² + 4ac",
            optionC = "4ac - b²",
            optionD = "b - 4ac",
            correctOption = "A",
            explanation = "द्विघात समीकरण ax² + bx + c = 0 का विविक्तकर D = b² - 4ac होता है।",
            source = "BSEB Class 10 Mathematics Ch. 4"
        ),
        QuizQuestion(
            id = "bseb_10_math_004",
            questionClass = 10,
            subject = "Mathematics",
            chapter = "Coordinate Geometry",
            questionText = "मूल बिंदु (Origin) के निर्देशांक क्या होते हैं?",
            optionA = "(1, 1)",
            optionB = "(0, 0)",
            optionC = "(0, 1)",
            optionD = "(1, 0)",
            correctOption = "B",
            explanation = "जहाँ X-अक्ष और Y-अक्ष एक-दूसरे को काटते हैं, उस मूल बिंदु के निर्देशांक (0, 0) होते हैं।",
            source = "BSEB Class 10 Mathematics Ch. 7"
        ),
        // --- CLASS 9 MATHEMATICS ---
        QuizQuestion(
            id = "bseb_9_math_001",
            questionClass = 9,
            subject = "Mathematics",
            chapter = "Number Systems",
            questionText = "π (पाई) एक कैसी संख्या है?",
            optionA = "परिमेय संख्या",
            optionB = "अपरिमेय संख्या",
            optionC = "पूर्णांक",
            optionD = "प्राकृत संख्या",
            correctOption = "B",
            explanation = "π का दशमलव प्रसार अनवसानी अनावर्ती होता है, इसलिए यह एक अपरिमेय संख्या है।",
            source = "BSEB Class 9 Mathematics Ch. 1"
        ),
        QuizQuestion(
            id = "bseb_9_math_002",
            questionClass = 9,
            subject = "Mathematics",
            chapter = "Heron's Formula",
            questionText = "हीरोन के सूत्र में त्रिभुज का अर्धपरिमाप s बराबर होता है:",
            optionA = "a + b + c",
            optionB = "(a + b + c) / 2",
            optionC = "2(a + b + c)",
            optionD = "a × b × c",
            correctOption = "B",
            explanation = "अर्धपरिमाप s = (a + b + c) / 2 होता है।",
            source = "BSEB Class 9 Mathematics Ch. 12"
        ),
        // --- CLASS 11 & 12 MATHEMATICS ---
        QuizQuestion(
            id = "bseb_11_math_001",
            questionClass = 11,
            subject = "Mathematics",
            chapter = "Sets",
            questionText = "यदि समुच्चय A में 3 अवयव हैं, तो A के उपसमुच्चयों (Subsets) की कुल संख्या क्या होगी?",
            optionA = "3",
            optionB = "6",
            optionC = "8",
            optionD = "9",
            correctOption = "C",
            explanation = "यदि n(A) = n, तो कुल उपसमुच्चयों की संख्या 2ⁿ = 2³ = 8 होती है।",
            source = "BSEB Class 11 Mathematics Ch. 1"
        ),
        QuizQuestion(
            id = "bseb_12_math_001",
            questionClass = 12,
            subject = "Mathematics",
            chapter = "Derivatives",
            questionText = "d/dx (sin x) का मान क्या होता है?",
            optionA = "-cos x",
            optionB = "cos x",
            optionC = "tan x",
            optionD = "sec² x",
            correctOption = "B",
            explanation = "sin x का x के सापेक्ष अवकलन (Derivative) cos x होता है।",
            source = "BSEB Class 12 Mathematics Ch. 5"
        ),
        QuizQuestion(
            id = "bseb_12_math_002",
            questionClass = 12,
            subject = "Mathematics",
            chapter = "Integrals",
            questionText = "∫ eˣ dx का मान क्या होता है?",
            optionA = "eˣ + C",
            optionB = "x·eˣ + C",
            optionC = "log x + C",
            optionD = "0",
            correctOption = "A",
            explanation = "चरघातांकी फलन eˣ का समाकलन eˣ + C होता है।",
            source = "BSEB Class 12 Mathematics Ch. 7"
        ),

        // --- PHYSICS (Class 9, 10, 11, 12) ---
        QuizQuestion(
            id = "bseb_11_phy_001",
            questionClass = 11,
            subject = "Physics",
            chapter = "Motion",
            questionText = "वेग का SI मात्रक क्या है?",
            optionA = "मीटर",
            optionB = "मीटर/सेकंड",
            optionC = "न्यूटन",
            optionD = "जूल",
            correctOption = "B",
            explanation = "वेग = विस्थापन / समय, अतः इसका SI मात्रक मीटर/सेकंड (m/s) होता है।",
            source = "BSEB Class 11 Physics Ch. 3"
        ),
        QuizQuestion(
            id = "bseb_10_phy_001",
            questionClass = 10,
            subject = "Physics",
            chapter = "Electricity",
            questionText = "विद्युत धारा का SI मात्रक क्या होता है?",
            optionA = "वोल्ट",
            optionB = "वाट",
            optionC = "एम्पीयर",
            optionD = "ओम",
            correctOption = "C",
            explanation = "विद्युत धारा को एम्पीयर (Ampere) में मापा जाता है।",
            source = "BSEB Class 10 Science Physics Ch. 12"
        ),
        QuizQuestion(
            id = "bseb_10_phy_002",
            questionClass = 10,
            subject = "Physics",
            chapter = "Light – Reflection and Refraction",
            questionText = "समतल दर्पण की फोकस दूरी होती है:",
            optionA = "शून्य",
            optionB = "अनंत",
            optionC = "25 सेमी",
            optionD = "-25 सेमी",
            correctOption = "B",
            explanation = "समतल दर्पण की वक्रता त्रिज्या अनंत होती है, इसलिए फोकस दूरी भी अनंत होती है।",
            source = "BSEB Class 10 Science Physics Ch. 10"
        ),
        QuizQuestion(
            id = "bseb_9_phy_001",
            questionClass = 9,
            subject = "Physics",
            chapter = "Force and Laws of Motion",
            questionText = "बल (Force) का SI मात्रक क्या है?",
            optionA = "पास्कल",
            optionB = "न्यूटन",
            optionC = "जूल",
            optionD = "वाट",
            correctOption = "B",
            explanation = "न्यूटन के द्वितीय नियम F = m·a के अनुसार बल का SI मात्रक न्यूटन (N) है।",
            source = "BSEB Class 9 Science Ch. 9"
        ),
        QuizQuestion(
            id = "bseb_12_phy_001",
            questionClass = 12,
            subject = "Physics",
            chapter = "Electrostatics",
            questionText = "विद्युत आवेश का SI मात्रक क्या है?",
            optionA = "कूलॉम",
            optionB = "वेबर",
            optionC = "फैराडे",
            optionD = "हेनरी",
            correctOption = "A",
            explanation = "विद्युत आवेश (Electric Charge) का SI मात्रक कूलॉम (Coulomb) होता है।",
            source = "BSEB Class 12 Physics Ch. 1"
        ),

        // --- CHEMISTRY (Class 9, 10, 11, 12) ---
        QuizQuestion(
            id = "bseb_11_chem_001",
            questionClass = 11,
            subject = "Chemistry",
            chapter = "Some Basic Concepts of Chemistry",
            questionText = "पानी का रासायनिक सूत्र क्या है?",
            optionA = "CO₂",
            optionB = "H₂O",
            optionC = "O₂",
            optionD = "NaCl",
            correctOption = "B",
            explanation = "जल के एक अणु में दो हाइड्रोजन परमाणु और एक ऑक्सीजन परमाणु होते हैं (H₂O)।",
            source = "BSEB Class 11 Chemistry Ch. 1"
        ),
        QuizQuestion(
            id = "bseb_10_chem_001",
            questionClass = 10,
            subject = "Chemistry",
            chapter = "Acids, Bases and Salts",
            questionText = "शुद्ध जल का pH मान कितना होता है?",
            optionA = "0",
            optionB = "5",
            optionC = "7",
            optionD = "14",
            correctOption = "C",
            explanation = "शुद्ध जल उदासीन होता है, अतः इसका pH मान 7 होता है।",
            source = "BSEB Class 10 Science Chemistry Ch. 2"
        ),
        QuizQuestion(
            id = "bseb_9_chem_001",
            questionClass = 9,
            subject = "Chemistry",
            chapter = "Atoms and Molecules",
            questionText = "कार्बन का परमाणु क्रमांक (Atomic Number) कितना होता है?",
            optionA = "4",
            optionB = "6",
            optionC = "8",
            optionD = "12",
            correctOption = "B",
            explanation = "कार्बन के नाभिक में 6 प्रोटॉन होते हैं, अतः इसका परमाणु क्रमांक 6 है।",
            source = "BSEB Class 9 Science Ch. 3"
        ),
        QuizQuestion(
            id = "bseb_12_chem_001",
            questionClass = 12,
            subject = "Chemistry",
            chapter = "Solutions",
            questionText = "मोलरता (Molarity) का मात्रक क्या होता है?",
            optionA = "मोल / किग्रा",
            optionB = "मोल / लीटर",
            optionC = "ग्राम / लीटर",
            optionD = "मात्रकहीन",
            correctOption = "B",
            explanation = "1 लीटर विलयन में घुले विलेय के मोलों की संख्या मोलरता कहलाती है (mol/L)।",
            source = "BSEB Class 12 Chemistry Ch. 2"
        ),

        // --- BIOLOGY (Class 9, 10, 11, 12) ---
        QuizQuestion(
            id = "bseb_12_bio_001",
            questionClass = 12,
            subject = "Biology",
            chapter = "Human Health and Endocrinology",
            questionText = "मानव शरीर में इंसुलिन किस अंग द्वारा बनाया जाता है?",
            optionA = "हृदय",
            optionB = "अग्न्याशय",
            optionC = "फेफड़ा",
            optionD = "गुर्दा",
            correctOption = "B",
            explanation = "इंसुलिन हार्मोन अग्न्याशय (Pancreas) की लैंगरहैंस द्वीपिकाओं की बीटा कोशिकाओं से स्रावित होता है।",
            source = "BSEB Class 12 Biology"
        ),
        QuizQuestion(
            id = "bseb_10_bio_001",
            questionClass = 10,
            subject = "Biology",
            chapter = "Life Processes",
            questionText = "पौधों में प्रकाश संश्लेषण की क्रिया किस अंगक में होती है?",
            optionA = "माइटोकॉन्ड्रिया",
            optionB = "हरितलवक (Chloroplast)",
            optionC = "राइबोसोम",
            optionD = "केंद्रक",
            correctOption = "B",
            explanation = "हरितलवक (Chloroplast) में उपस्थित क्लोरोफिल सूर्य के प्रकाश में भोजन बनाता है।",
            source = "BSEB Class 10 Science Biology Ch. 6"
        ),
        QuizQuestion(
            id = "bseb_9_bio_001",
            questionClass = 9,
            subject = "Biology",
            chapter = "The Fundamental Unit of Life",
            questionText = "कोशिका का 'पावर हाउस' (Powerhouse of Cell) किसे कहा जाता है?",
            optionA = "गॉल्जीकाय",
            optionB = "लाइसोसोम",
            optionC = "माइटोकॉन्ड्रिया",
            optionD = "राइबोसोम",
            correctOption = "C",
            explanation = "माइटोकॉन्ड्रिया ATP के रूप में ऊर्जा उत्पन्न करता है।",
            source = "BSEB Class 9 Science Ch. 5"
        ),
        QuizQuestion(
            id = "bseb_11_bio_001",
            questionClass = 11,
            subject = "Biology",
            chapter = "Cell Cycle",
            questionText = "DNA की द्वि-कुंडलिनी संरचना (Double Helix) किसने दी थी?",
            optionA = "मेंडल",
            optionB = "वाटसन और क्रिक",
            optionC = "डार्विन",
            optionD = "रॉबर्ट हुक",
            correctOption = "B",
            explanation = "जेम्स वाटसन और फ्रांसिस क्रिक ने 1953 में DNA की डबल हेलिक्स संरचना प्रस्तुत की।",
            source = "BSEB Class 11 Biology"
        ),

        // --- HINDI (Class 9, 10, 11, 12) ---
        QuizQuestion(
            id = "bseb_10_hin_001",
            questionClass = 10,
            subject = "Hindi",
            chapter = "Hindi Grammar – Sangya",
            questionText = "संज्ञा किसे कहते हैं?",
            optionA = "किसी व्यक्ति, वस्तु, स्थान या भाव के नाम को",
            optionB = "किसी काम को",
            optionC = "किसी विशेषता को",
            optionD = "किसी संबंध को",
            correctOption = "A",
            explanation = "किसी व्यक्ति, वस्तु, स्थान, प्राणी या भाव के नाम को संज्ञा कहते हैं।",
            source = "BSEB Class 10 Hindi Vyakaran"
        ),
        QuizQuestion(
            id = "bseb_10_hin_002",
            questionClass = 10,
            subject = "Hindi",
            chapter = "Sandhi",
            questionText = "'हिमालय' शब्द का सही संधि-विच्छेद क्या है?",
            optionA = "हिमा + लय",
            optionB = "हिम + आलय",
            optionC = "हि + मालय",
            optionD = "हिम + लय",
            correctOption = "B",
            explanation = "हिम + आलय = हिमालय (दीर्घ स्वर संधि)।",
            source = "BSEB Class 10 Hindi Vyakaran"
        ),
        QuizQuestion(
            id = "bseb_9_hin_001",
            questionClass = 9,
            subject = "Hindi",
            chapter = "Gadya Khand",
            questionText = "'दो बैलों की कथा' किसकी प्रसिद्ध रचना है?",
            optionA = "जयशंकर प्रसाद",
            optionB = "मुंशी प्रेमचंद",
            optionC = "महादेवी वर्मा",
            optionD = "रामधारी सिंह दिनकर",
            correctOption = "B",
            explanation = "'दो बैलों की कथा' मुंशी प्रेमचंद द्वारा रचित प्रसिद्ध कहानी है।",
            source = "BSEB Class 9 Hindi Godhuli"
        ),
        QuizQuestion(
            id = "bseb_12_hin_001",
            questionClass = 12,
            subject = "Hindi",
            chapter = "Digant Bhag 2",
            questionText = "'अर्धनारीश्वर' निबंध के लेखक कौन हैं?",
            optionA = "रामधारी सिंह 'दिनकर'",
            optionB = "नागार्जुन",
            optionC = "अज्ञेय",
            optionD = "निराला",
            correctOption = "A",
            explanation = "'अर्धनारीश्वर' निबंध राष्ट्रकवि रामधारी सिंह 'दिनकर' द्वारा रचित है।",
            source = "BSEB Class 12 Hindi Digant"
        ),
        QuizQuestion(
            id = "bseb_11_hin_001",
            questionClass = 11,
            subject = "Hindi",
            chapter = "Samas",
            questionText = "'रसोईघर' में कौन-सा समास है?",
            optionA = "द्वंद्व समास",
            optionB = "तत्पुरुष समास",
            optionC = "बहुव्रीहि समास",
            optionD = "अव्ययीभाव समास",
            correctOption = "B",
            explanation = "'रसोई के लिए घर' — इसमें संप्रदान तत्पुरुष समास है।",
            source = "BSEB Class 11 Hindi Vyakaran"
        ),

        // --- ENGLISH (Class 9, 10, 11, 12) ---
        QuizQuestion(
            id = "bseb_10_eng_001",
            questionClass = 10,
            subject = "English",
            chapter = "Grammar – Number",
            questionText = "What is the plural of Child?",
            optionA = "Childs",
            optionB = "Children",
            optionC = "Childes",
            optionD = "Childrens",
            correctOption = "B",
            explanation = "'Child' takes the irregular plural form 'Children'.",
            source = "BSEB Class 10 English Grammar"
        ),
        QuizQuestion(
            id = "bseb_10_eng_002",
            questionClass = 10,
            subject = "English",
            chapter = "Active & Passive Voice",
            questionText = "Choose the correct Passive Voice of: 'Ram writes a letter.'",
            optionA = "A letter is written by Ram.",
            optionB = "A letter was written by Ram.",
            optionC = "A letter has written by Ram.",
            optionD = "Ram is writing a letter.",
            correctOption = "A",
            explanation = "Simple Present tense 'writes' converts to 'is written by' in passive voice.",
            source = "BSEB Class 10 English Grammar"
        ),
        QuizQuestion(
            id = "bseb_9_eng_001",
            questionClass = 9,
            subject = "English",
            chapter = "Articles",
            questionText = "Fill in the blank: He is ___ honest man.",
            optionA = "a",
            optionB = "an",
            optionC = "the",
            optionD = "no article",
            correctOption = "B",
            explanation = "'Honest' begins with a silent 'h' and a vowel sound, so 'an' is used.",
            source = "BSEB Class 9 English Panorama"
        ),
        QuizQuestion(
            id = "bseb_11_eng_001",
            questionClass = 11,
            subject = "English",
            chapter = "Prepositions",
            questionText = "Choose the correct preposition: She is fond ___ music.",
            optionA = "on",
            optionB = "in",
            optionC = "of",
            optionD = "at",
            correctOption = "C",
            explanation = "The adjective 'fond' is always followed by the preposition 'of'.",
            source = "BSEB Class 11 English Grammar"
        ),
        QuizQuestion(
            id = "bseb_12_eng_001",
            questionClass = 12,
            subject = "English",
            chapter = "Rainbow Part II – Prose",
            questionText = "Who wrote the essay 'Indian Civilization and Culture'?",
            optionA = "Dr. Zakir Hussain",
            optionB = "Mahatma Gandhi",
            optionC = "Jawaharlal Nehru",
            optionD = "Bertrand Russell",
            correctOption = "B",
            explanation = "'Indian Civilization and Culture' is written by Mahatma Gandhi.",
            source = "BSEB Class 12 English Rainbow Part II"
        )
    )
}
