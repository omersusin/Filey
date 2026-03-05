package filey.app.feature.search.semantic.domain.usecase

class QueryPreprocessor {

    data class QueryInterpretation(
        val originalQuery: String,
        val normalizedQuery: String,
        val detectedIntent: SearchIntent,
        val extractedFilters: Map<String, String>
    )

    enum class SearchIntent {
        FIND_DOCUMENT,
        FIND_BY_DATE,
        FIND_BY_CONTENT,
        FIND_BY_TYPE,
        GENERAL
    }

    fun analyze(query: String): QueryInterpretation {
        val normalized = query.lowercase().trim()

        val intent = when {
            normalized.containsAny("nerede", "bul", "ara", "hangi") -> SearchIntent.FIND_DOCUMENT
            normalized.containsAny("tarih", "ay", "yıl", "günü") -> SearchIntent.FIND_BY_DATE
            normalized.containsAny("yazan", "içeren", "hakkında", "ilgili") -> SearchIntent.FIND_BY_CONTENT
            normalized.containsAny("pdf", "resim", "video", "dosya", "belge") -> SearchIntent.FIND_BY_TYPE
            else -> SearchIntent.GENERAL
        }

        val filters = mutableMapOf<String, String>()
        
        // Month Year detection
        val monthYearRegex = Regex(
            """(ocak|şubat|mart|nisan|mayıs|haziran|temmuz|ağustos|eylül|ekim|kasım|aralık)\s*(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        monthYearRegex.find(normalized)?.let { match ->
            val monthName = match.groupValues[1].lowercase()
            val year = match.groupValues[2]
            val monthNum = turkishMonthToNumber(monthName)
            filters["date"] = "$year-${monthNum.toString().padStart(2, '0')}"
        }
        
        val stopwords = setOf(
            "bir", "bu", "şu", "ve", "ile", "için", "de", "da",
            "mı", "mi", "nerede", "bul", "bana", "benim", "benım"
        )
        
        val cleanedQuery = normalized.split(" ")
            .filter { it !in stopwords && it.length > 1 }
            .joinToString(" ")

        return QueryInterpretation(
            originalQuery = query,
            normalizedQuery = cleanedQuery.ifEmpty { normalized },
            detectedIntent = intent,
            extractedFilters = filters
        )
    }

    private fun turkishMonthToNumber(month: String): Int {
        return when (month) {
            "ocak" -> 1
            "şubat" -> 2
            "mart" -> 3
            "nisan" -> 4
            "mayıs" -> 5
            "haziran" -> 6
            "temmuz" -> 7
            "ağustos" -> 8
            "eylül" -> 9
            "ekim" -> 10
            "kasım" -> 11
            "aralık" -> 12
            else -> 1
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
