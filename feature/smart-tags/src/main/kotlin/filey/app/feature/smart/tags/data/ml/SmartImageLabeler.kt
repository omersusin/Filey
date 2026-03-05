package filey.app.feature.smart.tags.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import filey.app.core.model.FileModel
import filey.app.feature.smart.tags.domain.model.SmartTag
import filey.app.feature.smart.tags.domain.model.TagCategory
import filey.app.feature.smart.tags.domain.model.TagSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class SmartImageLabeler(
    private val context: Context
) {
    
    private val labeler: ImageLabeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.65f)
            .build()
        ImageLabeling.getClient(options)
    }
    
    suspend fun label(file: FileModel): List<SmartTag> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = decodeSampledBitmap(file.path, 640, 640)
                    ?: return@withContext emptyList()
                
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                
                val labels = Tasks.await(labeler.process(inputImage), 10, TimeUnit.SECONDS)
                
                bitmap.recycle()
                
                labels.map { label ->
                    SmartTag(
                        value = label.text,
                        category = TagCategory.VISUAL_CONTENT,
                        confidence = label.confidence,
                        source = TagSource.ML_IMAGE_LABEL,
                        metadata = mapOf("index" to label.index.toString())
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun labelVideoThumbnail(file: FileModel): List<SmartTag> {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.path)
                val bitmap = retriever.getFrameAtTime(
                    1_000_000, // 1 second
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: return@withContext emptyList()
                
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val labels = Tasks.await(labeler.process(inputImage), 10, TimeUnit.SECONDS)
                bitmap.recycle()
                
                labels.map { label ->
                    SmartTag(
                        value = label.text,
                        category = TagCategory.VIDEO_CONTENT,
                        confidence = label.confidence * 0.8f, // thumbnail penalty
                        source = TagSource.ML_VIDEO_THUMBNAIL
                    )
                }
            } catch (e: Exception) {
                emptyList()
            } finally {
                retriever.release()
            }
        }
    }
    
    private fun decodeSampledBitmap(
        path: String, 
        reqWidth: Int, 
        reqHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options, 
        reqWidth: Int, 
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
