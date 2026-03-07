package filey.app.core.model

enum class FileType {
    IMAGE, VIDEO, AUDIO, TEXT, ARCHIVE, UNKNOWN;

    companion object {
        private val IMAGE_EXT   = setOf("jpg","jpeg","png","gif","bmp","webp","svg","heic","heif")
        private val VIDEO_EXT   = setOf("mp4","mkv","avi","mov","webm","flv","3gp","ts","wmv")
        private val AUDIO_EXT   = setOf("mp3","wav","flac","aac","ogg","m4a","wma","opus")
        private val TEXT_EXT    = setOf("txt","md","json","xml","csv","log","yaml","yml",
                                        "kt","java","py","js","ts","html","css","sh","conf","toml","ini")
        private val ARCHIVE_EXT = setOf("zip","tar","gz","bz2","7z","rar","xz")

        fun fromFileName(name: String): FileType {
            val ext = name.substringAfterLast('.', "").lowercase()
            return when {
                ext in IMAGE_EXT   -> IMAGE
                ext in VIDEO_EXT   -> VIDEO
                ext in AUDIO_EXT   -> AUDIO
                ext in TEXT_EXT    -> TEXT
                ext in ARCHIVE_EXT -> ARCHIVE
                else               -> UNKNOWN
            }
        }
    }
}
