package filey.app

import android.app.Application
import filey.app.core.di.AppContainer
import filey.app.feature.search.semantic.data.indexer.IndexingWorker
import filey.app.feature.search.semantic.data.vectordb.MyObjectBox
import filey.app.feature.smart.tags.data.worker.SmartTaggingWorker

class FileyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
        
        // Initialize ObjectBox for Semantic Search
        AppContainer.Instance.boxStore = MyObjectBox.builder()
            .androidContext(this)
            .build()
        
        // Schedule periodic background tasks
        IndexingWorker.schedule(this)
        SmartTaggingWorker.schedule(this)
    }
}
