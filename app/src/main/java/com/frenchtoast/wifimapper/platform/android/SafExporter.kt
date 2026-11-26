package com.frenchtoast.wifimapper.platform.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object SafExporter {

  fun writeToUri(context: Context, uri: Uri, content: String): Result<Unit> {
    return runCatching {
      context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(content.toByteArray(Charsets.UTF_8))
        stream.flush()
      } ?: error("Unable to open destination file stream")
    }
  }

  fun shareViaSheet(
    context: Context,
    fileName: String,
    mimeType: String,
    content: String,
  ): Result<Unit> {
    return runCatching {
      val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
      val file = File(exportDir, fileName)
      file.writeText(content, Charsets.UTF_8)

      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
      )
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      val chooser = Intent.createChooser(shareIntent, "Share $fileName").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    }
  }
}
