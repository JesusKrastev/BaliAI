package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jesuskrastev.bali.R

/**
 * Copy and asset for one slide of the pre-paywall preview.
 *
 * @param imageRes drawable of the real app screenshot
 * @param headline short, punchy label for the feature shown
 * @param subheadline one-sentence benefit that expands on the headline
 * @param contentDescription accessibility label for the screenshot
 */
private data class PreviewSlideContent(
    @DrawableRes val imageRes: Int,
    val headline: String,
    val subheadline: String,
    val contentDescription: String
)

/** The 3 real screenshots, in the order they should build the case for buying. */
private val previewSlides = listOf(
    PreviewSlideContent(
        imageRes = R.drawable.screen_ask,
        headline = "Sal de dudas al instante",
        subheadline = "Pregúntale a la IA de Bali cualquier duda del teórico y recibe la respuesta al momento, día y noche.",
        contentDescription = "Chat de dudas con la IA de Bali"
    ),
    PreviewSlideContent(
        imageRes = R.drawable.screen_exam,
        headline = "Entrena con exámenes reales",
        subheadline = "Practica con preguntas oficiales del examen DGT tantas veces como necesites, sin límites.",
        contentDescription = "Pregunta de un examen real dentro de la app"
    ),
    PreviewSlideContent(
        imageRes = R.drawable.screen_home,
        headline = "Un poco cada día, hasta aprobar",
        subheadline = "Completa tu lección diaria, mantén la racha viva y mira cómo se acerca tu aprobado.",
        contentDescription = "Racha y progreso diario en la pantalla principal"
    )
)

/** Fixed device-chrome colors: the phone frame reads as physical hardware, not themed UI. */
private val FrameBezel = Color(0xFF15161A)
private val FrameRim = Color(0xFF3A3B40)
private val FrameButton = Color(0xFF2A2B30)

/**
 * Last onboarding screen before the paywall. Walks through 3 real screenshots of the app,
 * one per slide, so the purchase decision is made after glimpsing the product instead of
 * just reading persuasion copy.
 */
@Composable
fun OnboardingPreviewScreen() {
    val pagerState = rememberPagerState(pageCount = { previewSlides.size })

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            PreviewSlide(previewSlides[page])
        }

        PreviewPageIndicator(
            pageCount = previewSlides.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
    }
}

/**
 * One slide: headline and subheadline above, the screenshot below framed as a floating
 * phone mockup so it reads as a device photo rather than a raw image bleeding to the edges.
 *
 * @param content headline, subheadline, image and accessibility label for this slide
 */
@Composable
private fun PreviewSlide(content: PreviewSlideContent) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = content.headline,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.headlineMedium.fontSize * 1.15f,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Text(
            text = content.subheadline,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 20.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PhoneMockup(
                imageRes = content.imageRes,
                contentDescription = content.contentDescription,
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .fillMaxHeight(0.92f)
            )
        }
    }
}

/**
 * Renders a screenshot inside a physical-device frame: rounded bezel, side buttons and a
 * top camera cutout, so app screenshots read as photos of a phone rather than flat crops.
 *
 * @param imageRes drawable of the real app screenshot to place inside the frame
 * @param contentDescription accessibility label carried by the screenshot
 * @param modifier modifier applied to the outer frame; its bounds set the mockup's size
 */
@Composable
private fun PhoneMockup(
    @DrawableRes imageRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val frameShape = RoundedCornerShape(38.dp)
    val screenShape = RoundedCornerShape(30.dp)

    Box(
        modifier = modifier
            .aspectRatio(9f / 19.5f, matchHeightConstraintsFirst = true)
            .shadow(elevation = 28.dp, shape = frameShape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(FrameBezel, frameShape)
            .border(width = 2.dp, color = FrameRim, shape = frameShape)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .clip(screenShape)
        )

        // Camera cutout, centered at the top edge of the frame.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .size(10.dp)
                .background(Color.Black, CircleShape)
                .border(width = 1.dp, color = FrameRim, shape = CircleShape)
        )

        // Power button.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 2.dp)
                .size(width = 3.dp, height = 46.dp)
                .background(FrameButton, RoundedCornerShape(2.dp))
        )

        // Volume buttons.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-2).dp, y = (-36).dp)
                .size(width = 3.dp, height = 32.dp)
                .background(FrameButton, RoundedCornerShape(2.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-2).dp, y = 22.dp)
                .size(width = 3.dp, height = 32.dp)
                .background(FrameButton, RoundedCornerShape(2.dp))
        )
    }
}

/**
 * Dot indicator for the slide pager.
 *
 * @param pageCount total number of slides
 * @param currentPage zero-based index of the slide currently in view
 * @param modifier modifier applied to the row of dots
 */
@Composable
private fun PreviewPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 8.dp)
                    .background(
                        color = if (index == currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
            )
        }
    }
}
