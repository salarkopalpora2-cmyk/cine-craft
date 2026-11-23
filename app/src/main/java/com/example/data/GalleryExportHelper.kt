package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ZipExportVerification(
    val isValid: Boolean,
    val entryCount: Int,
    val totalByteSize: Long,
    val entryNames: List<String>,
    val errorMessage: String? = null
)

data class ExportedDownloadResult(
    val success: Boolean,
    val fileName: String,
    val locationLabel: String,
    val shareUriString: String,
    val mimeType: String,
    val byteSize: Long,
    val isZip: Boolean,
    val entryCount: Int = 1,
    val errorMessage: String? = null
)

object GalleryExportHelper {

    private const val DOWNLOAD_CHANNEL_ID = "cinecraft_downloads_channel"
    private const val DOWNLOAD_CHANNEL_NAME = "CineCraft Downloads"

    /**
     * Packs image entries into a non-empty, verified ZIP archive and validates every entry via ZipInputStream.
     */
    fun createAndVerifyZipArchive(
        entries: List<Pair<String, ByteArray>>,
        outZipFile: File
    ): ZipExportVerification {
        val validEntries = entries.filter { it.second.isNotEmpty() }
        if (validEntries.isEmpty()) {
            return ZipExportVerification(
                isValid = false,
                entryCount = 0,
                totalByteSize = 0L,
                entryNames = emptyList(),
                errorMessage = "কোনো ভ্যালিড ইমেজ পাওয়া যায়নি"
            )
        }

        outZipFile.parentFile?.mkdirs()
        val usedNames = mutableSetOf<String>()
        var packedCount = 0

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(outZipFile))).use { zos ->
                zos.setLevel(Deflater.BEST_SPEED)
                validEntries.forEachIndexed { idx, (rawName, bytes) ->
                    val sanitized = rawName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        .ifBlank { "shot_${idx + 1}.jpg" }
                    var entryName = if (sanitized.lowercase().endsWith(".jpg") || sanitized.lowercase().endsWith(".jpeg")) {
                        sanitized
                    } else {
                        "$sanitized.jpg"
                    }
                    while (!usedNames.add(entryName)) {
                        entryName = "${idx + 1}_${System.nanoTime()}_$sanitized"
                    }
                    zos.putNextEntry(ZipEntry(entryName))
                    zos.write(bytes)
                    zos.closeEntry()
                    packedCount++
                }
                zos.finish()
            }
        } catch (e: Exception) {
            outZipFile.delete()
            return ZipExportVerification(
                isValid = false,
                entryCount = 0,
                totalByteSize = 0L,
                entryNames = emptyList(),
                errorMessage = e.localizedMessage ?: "ZIP তৈরি করা ব্যর্থ হয়েছে"
            )
        }

        val verification = verifyZipArchive(outZipFile, expectedMinEntries = packedCount)
        if (!verification.isValid) {
            outZipFile.delete()
        }
        return verification
    }

    /**
     * Reads back the generated ZIP file using ZipInputStream to confirm non-empty, uncorrupted entries.
     */
    fun verifyZipArchive(zipFile: File, expectedMinEntries: Int = 1): ZipExportVerification {
        if (!zipFile.exists() || zipFile.length() <= 22L) {
            return ZipExportVerification(
                isValid = false,
                entryCount = 0,
                totalByteSize = zipFile.length(),
                entryNames = emptyList(),
                errorMessage = "ZIP ফাইল খালি বা অনুপস্থিত"
            )
        }

        val names = mutableListOf<String>()
        val buffer = ByteArray(8192)
        return try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    var entryBytes = 0L
                    while (true) {
                        val read = zis.read(buffer)
                        if (read <= 0) break
                        entryBytes += read
                    }
                    zis.closeEntry()
                    if (!entry.isDirectory && entryBytes <= 0L) {
                        return ZipExportVerification(
                            isValid = false,
                            entryCount = names.size,
                            totalByteSize = zipFile.length(),
                            entryNames = names,
                            errorMessage = "ZIP এর ভেতরের ফাইল খালি: ${entry.name}"
                        )
                    }
                    if (!entry.isDirectory) {
                        names.add(entry.name)
                    }
                }
            }
            val valid = names.size >= expectedMinEntries && names.isNotEmpty()
            ZipExportVerification(
                isValid = valid,
                entryCount = names.size,
                totalByteSize = zipFile.length(),
                entryNames = names,
                errorMessage = if (valid) null else "ZIP এ সব ফাইল পাওয়া যায়নি (${names.size}/$expectedMinEntries)"
            )
        } catch (e: Exception) {
            ZipExportVerification(
                isValid = false,
                entryCount = names.size,
                totalByteSize = zipFile.length(),
                entryNames = names,
                errorMessage = e.localizedMessage ?: "ZIP ফাইল corrupted"
            )
        }
    }

    /**
     * Saves a single JPG image to BOTH Phone Gallery (Pictures/CineCraft) and Downloads (Download/CineCraft)
     * on API 29+, or FileProvider + MediaScanner on older APIs, and posts a clickable system notification.
     */
    fun saveSingleImageToDevice(
        context: Context,
        fileName: String,
        jpegBytes: ByteArray
    ): ExportedDownloadResult {
        if (jpegBytes.isEmpty()) {
            return ExportedDownloadResult(
                success = false,
                fileName = fileName,
                locationLabel = "",
                shareUriString = "",
                mimeType = "image/jpeg",
                byteSize = 0L,
                isZip = false,
                errorMessage = "ইমেজ ডেটা খালি"
            )
        }

        // Write a clean FileProvider copy in cacheDir/exports for instant Open/Share/Save-As
        val exportCacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val shareFile = File(exportCacheDir, fileName)
        FileOutputStream(shareFile).use {
            it.write(jpegBytes)
            it.flush()
        }
        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )

        var primaryViewUri: Uri = shareUri
        var locationLabel = "Downloads/CineCraft/$fileName"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver

            // 1. Save to Pictures/CineCraft so it appears immediately in Phone Gallery / Photos
            val picValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CineCraft")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val picUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, picValues)
            if (picUri != null) {
                resolver.openOutputStream(picUri)?.use { out ->
                    out.write(jpegBytes)
                    out.flush()
                }
                picValues.clear()
                picValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(picUri, picValues, null, null)
                primaryViewUri = picUri
            }

            // 2. Also save to Download/CineCraft so it is visible in Files -> Downloads & Desktop MTP
            val dlValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "image/jpeg")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/CineCraft")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val dlUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, dlValues)
            if (dlUri != null) {
                resolver.openOutputStream(dlUri)?.use { out ->
                    out.write(jpegBytes)
                    out.flush()
                }
                dlValues.clear()
                dlValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(dlUri, dlValues, null, null)
            }
            locationLabel = "Gallery (Pictures/CineCraft) & Downloads/CineCraft"
        } else {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "CineCraft"
            ).apply { mkdirs() }
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use {
                it.write(jpegBytes)
                it.flush()
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(outFile.absolutePath),
                arrayOf("image/jpeg"),
                null
            )
            primaryViewUri = shareUri
            locationLabel = outFile.absolutePath
        }

        postDownloadNotification(
            context = context,
            title = "Image Downloaded",
            message = "$fileName (${formatByteSize(jpegBytes.size.toLong())}) saved to $locationLabel",
            targetUri = primaryViewUri,
            mimeType = "image/jpeg",
            notificationId = fileName.hashCode()
        )

        return ExportedDownloadResult(
            success = true,
            fileName = fileName,
            locationLabel = locationLabel,
            shareUriString = shareUri.toString(),
            mimeType = "image/jpeg",
            byteSize = jpegBytes.size.toLong(),
            isZip = false,
            entryCount = 1
        )
    }

    /**
     * Exports a verified ZIP file into Downloads/CineCraft and posts a clickable system notification.
     */
    fun saveZipToDevice(
        context: Context,
        zipFileName: String,
        verifiedTempZip: File,
        entryCount: Int
    ): ExportedDownloadResult {
        val verification = verifyZipArchive(verifiedTempZip, expectedMinEntries = entryCount.coerceAtLeast(1))
        if (!verification.isValid) {
            return ExportedDownloadResult(
                success = false,
                fileName = zipFileName,
                locationLabel = "",
                shareUriString = "",
                mimeType = "application/zip",
                byteSize = 0L,
                isZip = true,
                errorMessage = verification.errorMessage ?: "ZIP ভেরিফিকেশন ব্যর্থ"
            )
        }

        val exportCacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val shareZipFile = File(exportCacheDir, zipFileName)
        verifiedTempZip.copyTo(shareZipFile, overwrite = true)
        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareZipFile
        )

        var viewUri: Uri = shareUri
        val locationLabel: String

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, zipFileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/CineCraft")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val dlUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (dlUri != null) {
                resolver.openOutputStream(dlUri)?.use { out ->
                    FileInputStream(verifiedTempZip).use { input ->
                        input.copyTo(out)
                        out.flush()
                    }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(dlUri, values, null, null)
                viewUri = dlUri
                locationLabel = "Downloads/CineCraft/$zipFileName"
            } else {
                locationLabel = shareZipFile.absolutePath
            }
        } else {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "CineCraft"
            ).apply { mkdirs() }
            val destFile = File(dir, zipFileName)
            verifiedTempZip.copyTo(destFile, overwrite = true)
            locationLabel = "Downloads/CineCraft/${destFile.name}"
        }

        val byteSize = shareZipFile.length()
        postDownloadNotification(
            context = context,
            title = "ZIP Archive Ready ($entryCount files)",
            message = "$zipFileName (${formatByteSize(byteSize)}) saved to Downloads/CineCraft",
            targetUri = viewUri,
            mimeType = "application/zip",
            notificationId = zipFileName.hashCode()
        )

        return ExportedDownloadResult(
            success = true,
            fileName = zipFileName,
            locationLabel = locationLabel,
            shareUriString = shareUri.toString(),
            mimeType = "application/zip",
            byteSize = byteSize,
            isZip = true,
            entryCount = verification.entryCount
        )
    }

    fun postDownloadNotification(
        context: Context,
        title: String,
        message: String,
        targetUri: Uri,
        mimeType: String,
        notificationId: Int
    ) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val existing = manager.getNotificationChannel(DOWNLOAD_CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        DOWNLOAD_CHANNEL_ID,
                        DOWNLOAD_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Notifications for saved screenshots and ZIP exports"
                    }
                    manager.createNotificationChannel(channel)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(targetUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooserIntent = Intent.createChooser(viewIntent, title).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                chooserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: Exception) {
        }
    }

    fun formatByteSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(java.util.Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(java.util.Locale.US, "%.2f MB", mb)
    }
}
