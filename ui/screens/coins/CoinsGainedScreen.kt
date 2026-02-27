package com.jesuskrastev.bali.ui.screens.coins

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R

@Composable
fun CoinsGainedScreen(
    coinsGained: Int,
    onContinueClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            Button(
                onClick = onContinueClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFFF)) // Blue like the image
            ) {
                Text("Continue", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Mascot holding a bowl of coins
            Image(
                painter = painterResource(id = R.drawable.bali_coins), // Assuming you have an image for this
                contentDescription = null,
                modifier = Modifier.size(240.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Coin Icon (Using a simple yellow circle/emoji for now as per image)
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color(0xFFFFD700)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("•", color = Color(0xFFDAA520), fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "+$coinsGained",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFA500) // Orange/Gold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Recolecta Manna y cámbialo por herramientas, looks y más.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                lineHeight = 24.sp
            )
        }
    }
}
