package com.example.ui.components.design

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GradientPair
import com.example.ui.theme.ShareApkGradients

/** Icon + colour for a file, from its MIME type or extension. */
fun fileTypeVisual(fileName: String, mime: String?): Pair<ImageVector, GradientPair> {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    val m = mime.orEmpty()
    return when {
        ext in setOf("apk", "apks", "xapk", "apkm") -> Icons.Outlined.Android to ShareApkGradients.green
        m.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "gif", "heic") -> Icons.Outlined.Image to ShareApkGradients.violet
        m.startsWith("video/") || ext in setOf("mp4", "mkv", "mov", "webm", "3gp") -> Icons.Outlined.Movie to ShareApkGradients.rose
        m.startsWith("audio/") || ext in setOf("mp3", "m4a", "ogg", "wav", "flac", "opus") -> Icons.Outlined.Audiotrack to ShareApkGradients.amber
        m.startsWith("text/") || ext in setOf("txt", "md") -> Icons.Outlined.TextSnippet to ShareApkGradients.sky
        ext in setOf("zip", "rar", "7z", "tar", "gz") -> Icons.Outlined.FolderZip to ShareApkGradients.amber
        ext in setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx") -> Icons.Outlined.Description to ShareApkGradients.sky
        else -> Icons.Outlined.InsertDriveFile to ShareApkGradients.indigo
    }
}

@Composable
fun FileTypeIcon(fileName: String, mime: String?, modifier: Modifier = Modifier, size: Dp = 42.dp) {
    val (icon, gradient) = fileTypeVisual(fileName, mime)
    GradientIconBox(gradient, modifier, size) { Icon(icon, null, Modifier.size(size * 0.5f)) }
}
