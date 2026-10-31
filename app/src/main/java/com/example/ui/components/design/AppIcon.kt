package com.example.ui.components.design

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ShareApkTokens

/** Real launcher icon when available, otherwise a deterministic letter tile. */
@Composable
fun AppIcon(label: String, bitmap: Bitmap?, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    if (bitmap != null) {
        val image = remember(bitmap) { bitmap.asImageBitmap() }
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier.size(size).clip(RoundedCornerShape(ShareApkTokens.SmallRadius))
        )
    } else {
        LetterIconBox(label = label, modifier = modifier, size = size)
    }
}
