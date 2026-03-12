package filey.app.feature.dashboard.domain

sealed class CommandBarAction {
    // Doğal dil arama
    data class Search(val query: String) : CommandBarAction()
    
    // Hızlı aksiyonlar
    data class QuickAction(val action: String) : CommandBarAction()
    
    // Dosya navigasyonu
    data class Navigate(val path: String) : CommandBarAction()
    
    // Tag bazlı filtreleme
    data class FilterByTag(val tag: String) : CommandBarAction()
}
