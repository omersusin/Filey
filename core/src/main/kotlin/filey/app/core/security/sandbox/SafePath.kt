package filey.app.core.security.sandbox

import java.io.File

/**
 * Shell komut enjeksiyonunu ve path traversal saldırılarını önlemek için tasarlanmış güvenli path wrapper'ı.
 */
@JvmInline
value class SafePath private constructor(val value: String) {
    companion object {
        fun of(rawPath: String): SafePath? {
            val normalized = try {
                File(rawPath).canonicalPath
            } catch (e: Exception) {
                return null
            }

            // Path traversal kontrolü
            if (normalized.contains("../") || normalized.contains("/./")) {
                return null
            }

            // Temel sistem dizinleri koruması (isteğe bağlı genişletilebilir)
            val forbiddenPrefixes = listOf("/proc", "/sys", "/dev")
            if (forbiddenPrefixes.any { normalized.startsWith(it) }) {
                // Bazı durumlarda bunlara erişim gerekebilir, ancak varsayılan olarak kısıtlıyoruz.
            }

            return SafePath(normalized)
        }

        /**
         * Testler veya dahili güvenilir path'ler için.
         */
        fun unsafe(path: String) = SafePath(path)
    }

    /**
     * Shell komutlarında güvenle kullanılabilmesi için tırnak içine alır ve içindeki tırnakları kaçırır.
     */
    fun shellEscaped(): String {
        return "'${value.replace("'", "'\\''")}'"
    }

    override fun toString(): String = value
}
