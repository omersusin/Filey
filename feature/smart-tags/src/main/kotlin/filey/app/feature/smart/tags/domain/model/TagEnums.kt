package filey.app.feature.smart.tags.domain.model

enum class TagCategory {
    VISUAL_CONTENT,
    VIDEO_CONTENT,
    AUDIO_METADATA,
    DOCUMENT_TYPE,
    DATE,
    FINANCIAL,
    LOCATION,
    CONTENT_TYPE,
    FILENAME_INFERENCE
}

enum class TagSource {
    ML_IMAGE_LABEL,
    ML_VIDEO_THUMBNAIL,
    ML_OCR,
    ML_DOCUMENT_CLASSIFIER,
    EXIF_METADATA,
    AUDIO_METADATA,
    FILENAME_ANALYSIS
}
