package filey.app.feature.search.semantic.data.indexer

class BertTokenizer(
    private val vocab: List<String>,
    private val maxLength: Int = 128
) {
    private val vocabMap = vocab.withIndex().associate { it.value to it.index }
    private val UNK_ID = vocabMap["[UNK]"] ?: 0
    private val CLS_ID = vocabMap["[CLS]"] ?: 101
    private val SEP_ID = vocabMap["[SEP]"] ?: 102
    private val PAD_ID = vocabMap["[PAD]"] ?: 0

    fun tokenize(text: String): IntArray {
        val result = mutableListOf<Int>()
        result.add(CLS_ID)

        val words = text.lowercase().split(Regex("""\s+"""))
        for (word in words) {
            if (result.size >= maxLength - 1) break
            
            // Basic WordPiece-like splitting
            var current = word
            while (current.isNotEmpty() && result.size < maxLength - 1) {
                var found = false
                for (i in current.length downTo 1) {
                    val sub = current.substring(0, i)
                    val id = vocabMap[sub]
                    if (id != null) {
                        result.add(id)
                        current = current.substring(i)
                        if (current.isNotEmpty()) {
                            // Sub-word marker
                            current = "##" + current
                        }
                        found = true
                        break
                    }
                }
                if (!found) {
                    result.add(UNK_ID)
                    break
                }
            }
        }

        result.add(SEP_ID)
        
        // Padding
        val finalArray = IntArray(maxLength) { PAD_ID }
        result.take(maxLength).forEachIndexed { index, id ->
            finalArray[index] = id
        }
        
        return finalArray
    }
}
