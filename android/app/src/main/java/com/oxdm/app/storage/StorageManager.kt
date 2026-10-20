package com.oxdm.app.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

/**
 * Manages staging downloads and atomically publishing completed files into the default
 * emulated SD card Download directory (`/storage/emulated/0/Download`).
 */
class StorageManager(private val context: Context) {

    /**
     * Default public destination: /storage/emulated/0/Download
     */
    val defaultPublicDownloadDir: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    /**
     * Temporary staging directory where active chunks and segments are written.
     */
    val stagingDir: File
        get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "staging").apply { mkdirs() }

    fun getStagingFile(jobId: String, extension: String = "part"): File {
        return File(stagingDir, "$jobId.$extension")
    }

    /**
     * Publishes a fully downloaded file from staging into /storage/emulated/0/Download.
     * Uses MediaStore.Downloads on Android 10+ (API 29+) and direct filesystem write on older versions.
     */
    fun publishToPublicDownloads(
        stagingFile: File,
        targetFilename: String,
        mimeType: String = "application/octet-stream"
    ): Uri? {
        if (!stagingFile.exists()) return null

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, targetFilename)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val collectionUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collectionUri, contentValues)

            if (itemUri != null) {
                resolver.openOutputStream(itemUri)?.use { out ->
                    FileInputStream(stagingFile).use { input ->
                        input.copyTo(out)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
                stagingFile.delete()
                itemUri
            } else {
                null
            }
        } else {
            val destFile = File(defaultPublicDownloadDir, targetFilename)
            stagingFile.copyTo(destFile, overwrite = true)
            stagingFile.delete()
            Uri.fromFile(destFile)
        }
    }
}
