package filey.app.feature.search.semantic.data.indexer

import filey.app.feature.search.semantic.domain.model.EntityType
import filey.app.feature.search.semantic.domain.model.NamedEntity

class MetadataExtractor {

    fun extractEntities(text: String): List<NamedEntity> {
        val entities = mutableListOf<NamedEntity>()
        
        // Date patterns
        val datePatterns = listOf(
            """\d{2}[./]\d{2}[./]\d{4}""",
            """\d{4}-\d{2}-\d{2}""",
            """(?i)(ocak|şubat|mart|nisan|mayıs|haziran|temmuz|ağustos|eylül|ekim|kasım|aralık)\s+\d{4}"""
        )
        
        datePatterns.forEach { pattern ->
            Regex(pattern).findAll(text).forEach { match ->
                entities.add(
                    NamedEntity(
                        type = EntityType.DATE,
                        value = match.value,
                        position = match.range
                    )
                )
            }
        }
        
        // Invoice / Fatura pattern
        val invoicePattern = """(?i)(fatura|invoice|fiş)\s*(no|numarası|number)?[:\s#]*([A-Z0-9\-]+)"""
        Regex(invoicePattern).findAll(text).forEach { match ->
            if (match.groupValues.size >= 4) {
                entities.add(
                    NamedEntity(
                        type = EntityType.INVOICE_NUMBER,
                        value = match.groupValues[3],
                        position = match.range
                    )
                )
            }
        }
        
        // Money amounts
        val moneyPattern = """[\$€₺]\s*[\d,.]+|[\d,.]+\s*(TL|USD|EUR|₺)"""
        Regex(moneyPattern).findAll(text).forEach { match ->
            entities.add(
                NamedEntity(
                    type = EntityType.MONEY,
                    value = match.value,
                    position = match.range
                )
            )
        }
        
        return entities
    }
}
