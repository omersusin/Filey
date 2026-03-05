package filey.app.feature.smart.tags.data.ml

import filey.app.core.model.FileModel
import filey.app.feature.smart.tags.domain.model.SmartTag
import filey.app.feature.smart.tags.domain.model.TagCategory
import filey.app.feature.smart.tags.domain.model.TagSource

class DocumentClassifier {
    suspend fun classify(file: FileModel): List<SmartTag> {
        // Placeholder for custom TFLite document classification
        return emptyList()
    }
}
