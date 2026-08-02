package com.jesuskrastev.bali.ui.screens.onboarding.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Renders onboarding copy where segments wrapped in pipes are emphasised, so the
 * whole flow shares one convention: `"esto es |lo importante|"`.
 *
 * @param highlightColor colour applied to the emphasised segments
 * @return the styled string, with pipe characters removed
 */
fun String.highlightPipes(highlightColor: Color): AnnotatedString = buildAnnotatedString {
    split("|").forEachIndexed { index, part ->
        if (index % 2 == 1) {
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                append(part)
            }
        } else {
            append(part)
        }
    }
}
