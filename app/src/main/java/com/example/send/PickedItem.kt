package com.example.send

import android.net.Uri

/** An entry in the Files tab: a document/media URI (possibly inside a picked folder) or a text message. */
data class PickedItem(
    val id: Long,
    val name: String,
    val size: Long,
    val mimeType: String?,
    val uri: Uri? = null,
    val text: String? = null
) {
    val isText get() = text != null
}
