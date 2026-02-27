package com.jesuskrastev.bali.ui.screens.suggestions

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    onBackClick: () -> Unit,
    viewModel: SuggestionsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Enviar sugerencia", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isSuccess) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.8f))
                
                Image(
                    painter = painterResource(id = R.drawable.happy_bali),
                    contentDescription = null,
                    modifier = Modifier.size(160.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "¡Gracias por tu ayuda!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "Tus sugerencias nos ayudan a mejorar Bali cada día para que todos aprueben a la primera.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(Modifier.weight(1f))
                
                Button(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("VOLVER AL INICIO", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bali),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )
                
                Spacer(Modifier.height(16.dp))

                Text(
                    "Ayúdanos a mejorar Bali. Cuéntanos qué te gustaría ver en la app o qué podemos mejorar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = uiState.suggestion,
                    onValueChange = { viewModel.onSuggestionChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("Escribe aquí tu sugerencia...") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { viewModel.sendSuggestion() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = uiState.suggestion.isNotBlank() && !uiState.isLoading,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (uiState.isLoading) {
                        run {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        }
                    } else {
                        Icon(Icons.Rounded.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("ENVIAR SUGERENCIA", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
