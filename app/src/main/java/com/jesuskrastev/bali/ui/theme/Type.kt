package com.jesuskrastev.bali.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R

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

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Gantari,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Gantari,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Gantari,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
