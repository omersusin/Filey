package filey.app.core.model

enum class SortOption(val label: String) {
    NAME_ASC("Ad (A→Z)"),
    NAME_DESC("Ad (Z→A)"),
    SIZE_ASC("Boyut (küçük→büyük)"),
    SIZE_DESC("Boyut (büyük→küçük)"),
    DATE_ASC("Tarih (eski→yeni)"),
    DATE_DESC("Tarih (yeni→eski)"),
    TYPE_ASC("Tür (A→Z)")
}
