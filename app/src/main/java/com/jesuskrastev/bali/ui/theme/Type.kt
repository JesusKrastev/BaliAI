package com.jesuskrastev.bali.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R

/** Bali brand typeface, bundled in `res/font` with every weight from Thin to Black. */
val Gantari = FontFamily(
    Font(R.font.gantari_regular, FontWeight.Normal),
    Font(R.font.gantari_medium, FontWeight.Medium),
    Font(R.font.gantari_semibold, FontWeight.SemiBold),
    Font(R.font.gantari_bold, FontWeight.Bold),
    Font(R.font.gantari_light, FontWeight.Light),
    Font(R.font.gantari_thin, FontWeight.Thin),
    Font(R.font.gantari_black, FontWeight.Black),
    Font(R.font.gantari_extrabold, FontWeight.ExtraBold),
    Font(R.font.gantari_extralight, FontWeight.ExtraLight)
)

private val MaterialDefaults = Typography()

/**
 * Converts a Material3 default text style to [Gantari] without touching its size, weight,
 * line height or letter spacing.
 *
 * @receiver the default style to convert
 * @return the same style using [Gantari]
 */
private fun TextStyle.inGantari(): TextStyle = copy(fontFamily = Gantari)

/**
 * App typography: every Material3 style is set in [Gantari]. `bodyLarge`, `titleLarge` and
 * `labelSmall` keep their custom metrics; the rest keep the Material3 defaults.
 */
val Typography = Typography(
    displayLarge = MaterialDefaults.displayLarge.inGantari(),
    displayMedium = MaterialDefaults.displayMedium.inGantari(),
    displaySmall = MaterialDefaults.displaySmall.inGantari(),
    headlineLarge = MaterialDefaults.headlineLarge.inGantari(),
    headlineMedium = MaterialDefaults.headlineMedium.inGantari(),
    headlineSmall = MaterialDefaults.headlineSmall.inGantari(),
    titleLarge = TextStyle(
        fontFamily = Gantari,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = MaterialDefaults.titleMedium.inGantari(),
    titleSmall = MaterialDefaults.titleSmall.inGantari(),
    bodyLarge = TextStyle(
        fontFamily = Gantari,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = MaterialDefaults.bodyMedium.inGantari(),
    bodySmall = MaterialDefaults.bodySmall.inGantari(),
    labelLarge = MaterialDefaults.labelLarge.inGantari(),
    labelMedium = MaterialDefaults.labelMedium.inGantari(),
    labelSmall = TextStyle(
        fontFamily = Gantari,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
