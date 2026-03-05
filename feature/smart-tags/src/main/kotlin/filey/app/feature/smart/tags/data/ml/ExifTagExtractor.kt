package filey.app.feature.smart.tags.data.ml

import androidx.exifinterface.media.ExifInterface
import filey.app.core.model.FileModel
import filey.app.feature.smart.tags.domain.model.SmartTag
import filey.app.feature.smart.tags.domain.model.TagCategory
import filey.app.feature.smart.tags.domain.model.TagSource
import java.io.File

class ExifTagExtractor {
    fun extract(file: FileModel): List<SmartTag> {
        return try {
            val exif = ExifInterface(file.path)
            val tags = mutableListOf<SmartTag>()
            
            exif.getAttribute(ExifInterface.TAG_DATETIME)?.let {
                tags.add(SmartTag(it, TagCategory.DATE, 1.0f, TagSource.EXIF_METADATA))
            }
            
            exif.getAttribute(ExifInterface.TAG_MODEL)?.let {
                tags.add(SmartTag(it, TagCategory.VISUAL_CONTENT, 1.0f, TagSource.EXIF_METADATA))
            }
            
            val latLong = FloatArray(2)
            if (exif.getLatLong(latLong)) {
                tags.add(SmartTag("${latLong[0]},${latLong[1]}", TagCategory.LOCATION, 1.0f, TagSource.EXIF_METADATA))
            }
            
            tags
        } catch (e: Exception) {
            emptyList()
        }
    }
}
