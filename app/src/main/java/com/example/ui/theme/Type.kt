package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/** IBM Plex Sans Arabic (SIL OFL 1.1), bundled in res/font. Covers Arabic and Latin. */
val PlexArabic = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold)
)

private fun style(size: Float, weight: FontWeight, line: Float) = TextStyle(
    fontFamily = PlexArabic,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp
)

/** Type scale (spec §3.3). */
val Typography = Typography(
    headlineLarge = style(30f, FontWeight.Bold, 40f),
    headlineMedium = style(26f, FontWeight.Bold, 36f),   // screen title
    titleLarge = style(19f, FontWeight.Bold, 28f),
    titleMedium = style(16f, FontWeight.SemiBold, 24f),  // section title
    titleSmall = style(14.5f, FontWeight.SemiBold, 22f),
    bodyLarge = style(15f, FontWeight.SemiBold, 23f),    // list item title
    bodyMedium = style(14f, FontWeight.Normal, 22f),
    bodySmall = style(12.5f, FontWeight.Normal, 19f),    // secondary text
    labelLarge = style(14f, FontWeight.SemiBold, 20f),
    labelMedium = style(12.5f, FontWeight.Medium, 18f),
    labelSmall = style(11f, FontWeight.SemiBold, 16f)    // badges
)
