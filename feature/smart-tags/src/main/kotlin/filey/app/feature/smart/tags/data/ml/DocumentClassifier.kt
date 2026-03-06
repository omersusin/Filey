package filey.app.feature.smart.tags.data.ml

import filey.app.core.model.FileModel
import filey.app.feature.smart.tags.domain.model.SmartTag
import filey.app.feature.smart.tags.domain.model.TagCategory
import filey.app.feature.smart.tags.domain.model.TagSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DocumentClassifier {

    private val filenameRules = listOf(
        Regex("fatura|invoice|makbuz|receipt", RegexOption.IGNORE_CASE) to ("invoice" to 0.85f),
        Regex("sözleşme|sozlesme|contract|agreement", RegexOption.IGNORE_CASE) to ("contract" to 0.85f),
        Regex("cv|resume|özgeçmiş", RegexOption.IGNORE_CASE) to ("resume" to 0.85f),
        Regex("rapor|report|analiz", RegexOption.IGNORE_CASE) to ("report" to 0.80f),
        Regex("bütçe|butce|budget", RegexOption.IGNORE_CASE) to ("budget" to 0.80f),
        Regex("sigorta|insurance|poliçe", RegexOption.IGNORE_CASE) to ("insurance" to 0.80f),
        Regex("diploma|sertifika|certificate", RegexOption.IGNORE_CASE) to ("certificate" to 0.90f),
        Regex("reçete|recete|prescription", RegexOption.IGNORE_CASE) to ("prescription" to 0.90f),
        Regex("banka|bank|ekstre|statement", RegexOption.IGNORE_CASE) to ("bank_statement" to 0.80f),
    )

    private val extensionTags = mapOf(
        "pdf"  to SmartTag("pdf_document",  TagCategory.CONTENT_TYPE, 1.0f, TagSource.FILENAME_ANALYSIS),
        "docx" to SmartTag("word_document", TagCategory.CONTENT_TYPE, 1.0f, TagSource.FILENAME_ANALYSIS),
        "xlsx" to SmartTag("spreadsheet",   TagCategory.CONTENT_TYPE, 1.0f, TagSource.FILENAME_ANALYSIS),
        "pptx" to SmartTag("presentation",  TagCategory.CONTENT_TYPE, 1.0f, TagSource.FILENAME_ANALYSIS),
        "txt"  to SmartTag("text_file",     TagCategory.CONTENT_TYPE, 1.0f, TagSource.FILENAME_ANALYSIS),
        "md"   to SmartTag("markdown",      TagCategory.CONTENT_TYPE, 1.0f, TagSource.FILENAME_ANALYSIS),
        "csv"  to SmartTag("spreadsheet",   TagCategory.CONTENT_TYPE, 0.9f, TagSource.FILENAME_ANALYSIS),
    )

    suspend fun classify(file: FileModel): List<SmartTag> = withContext(Dispatchers.Default) {
        val tags = mutableListOf<SmartTag>()
        extensionTags[file.extension.lowercase()]?.let { tags.add(it) }
        val nameNoExt = file.name.substringBeforeLast(".")
        filenameRules.forEach { (regex, info) ->
            if (regex.containsMatchIn(nameNoExt))
                tags.add(SmartTag(info.first, TagCategory.DOCUMENT_TYPE, info.second, TagSource.FILENAME_ANALYSIS))
        }
        Regex("""\d{4}[-._]\d{2}[-._]\d{2}""").find(file.name)?.let {
            tags.add(SmartTag(it.value.replace(Regex("[-._]"), "-"), TagCategory.DATE, 0.95f, TagSource.FILENAME_ANALYSIS))
        }
        if (file.extension.equals("pdf", ignoreCase = true) && file.size > 5 * 1024 * 1024)
            tags.add(SmartTag("scanned_document", TagCategory.CONTENT_TYPE, 0.6f, TagSource.FILENAME_ANALYSIS))
        tags
    }
}
