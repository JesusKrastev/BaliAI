package com.jesuskrastev.bali.ui.screens.onboarding.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jesuskrastev.bali.ui.screens.onboarding.OnboardingConfig
import com.jesuskrastev.bali.ui.screens.onboarding.components.highlightPipes
import com.jesuskrastev.bali.ui.theme.BaliTheme

private val FIELD_HEIGHT = 64.dp
private val ROW_HEIGHT = 56.dp

/**
 * Asks which traffic office the user will sit the exam in.
 *
 * A field plus a modal picker, which is what every app uses for a list this long: the step
 * itself stays a single, obvious control showing the current answer, and the 52 provinces
 * live in a sheet with its own search. Changing a wrong pick is the same gesture as making
 * it — tap the field, tap another province — so nothing has to be cleared by hand.
 *
 * The note above the field says the true thing: the theory exam is the same in every province,
 * so Bali covers all 52. Once a province is picked, a line confirms it by name, which is the
 * personal touch this screen is for — the old claim that every traffic office has its own
 * question bank was false and is gone, together with the screen that repeated it.
 *
 * @param selectedProvince the province chosen so far, or null if none
 * @param onSelect called with the province the user picked
 */
@Composable
fun StepProvince(
    selectedProvince: String?,
    onSelect: (String) -> Unit
) {
    var pickerOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        CoverageNote()

        Spacer(modifier = Modifier.height(16.dp))

        ProvinceField(
            province = selectedProvince,
            onClick = { pickerOpen = true }
        )

        AnimatedVisibility(visible = selectedProvince != null, enter = fadeIn() + expandVertically()) {
            Text(
                text = "✅ Listo: te preparas para el examen de la DGT en |${selectedProvince.orEmpty()}|."
                    .highlightPipes(MaterialTheme.colorScheme.primary),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 14.dp, start = 4.dp, end = 4.dp)
            )
        }
    }

    if (pickerOpen) {
        ProvincePicker(
            selectedProvince = selectedProvince,
            onDismiss = { pickerOpen = false },
            onSelect = { province ->
                onSelect(province)
                pickerOpen = false
            }
        )
    }
}

private const val COVERAGE_NOTE = "Preparamos el teórico en |las 52 provincias de España|: " +
    "el examen de la DGT es el mismo en todas, estés donde estés."

/** Says that Bali covers every province, and why that is true: the exam is the same in all. */
@Composable
private fun CoverageNote() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🇪🇸", fontSize = 26.sp)
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = COVERAGE_NOTE.highlightPipes(MaterialTheme.colorScheme.primary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * The control on the step itself: a field that reads as empty until answered and as the
 * answer afterwards, always tappable to change it.
 *
 * @param province the current answer, or null if none
 * @param onClick opens the picker
 */
@Composable
private fun ProvinceField(province: String?, onClick: () -> Unit) {
    val answered = province != null

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(FIELD_HEIGHT),
        shape = RoundedCornerShape(16.dp),
        color = if (answered) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (answered) 2.dp else 1.5.dp,
            color = if (answered) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (answered) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (answered) {
                    Text(
                        text = "TU PROVINCIA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = province ?: "Selecciona tu provincia",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (answered) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.UnfoldMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Modal list of every province, with its own search box and a header per initial.
 *
 * The search is not focused on open: the keyboard would cover most of the list, and for a
 * list this short scrolling is usually faster than typing. The headers are what make that
 * scroll workable — they turn 52 identical rows into a dozen labelled sections.
 *
 * @param selectedProvince the current answer, ticked in the list
 * @param onDismiss called when the sheet is closed without choosing
 * @param onSelect called with the province the user tapped
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ProvincePicker(
    selectedProvince: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val sections = remember(query) { OnboardingConfig.provincesByInitial(query) }
    val listState = rememberLazyListState()

    // Reopening the sheet must land on the answer, not at the top of the alphabet.
    LaunchedEffect(Unit) {
        val index = selectedProvince?.let { province ->
            var offset = 0
            for ((_, group) in OnboardingConfig.provincesByInitial("")) {
                offset++ // the section header
                val position = group.indexOf(province)
                if (position >= 0) return@let offset + position
                offset += group.size
            }
            null
        }
        if (index != null) listState.scrollToItem(index)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Elige tu provincia",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            SearchField(query = query, onQueryChange = { query = it })

            Spacer(modifier = Modifier.height(12.dp))

            if (sections.isEmpty()) {
                NoMatches()
                return@Column
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                sections.forEach { (initial, group) ->
                    stickyHeader(key = "header_$initial") {
                        SectionHeader(initial = initial)
                    }
                    items(group, key = { it }) { province ->
                        ProvinceRow(
                            province = province,
                            isSelected = province == selectedProvince,
                            onClick = { onSelect(province) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Search box of the picker.
 *
 * @param query the current text
 * @param onQueryChange called with every edit
 */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Borrar búsqueda",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(onSearch = {}),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    )
}

/**
 * Initial that labels a section of the list.
 *
 * @param initial the letter the provinces below start with
 */
@Composable
private fun SectionHeader(initial: Char) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = initial.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
    }
}

/**
 * One province of the list.
 *
 * @param province the province name
 * @param isSelected true when this is the current answer
 * @param onClick selects this province
 */
@Composable
private fun ProvinceRow(province: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = province,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Shown when the search matches nothing, so the list never just goes blank. */
@Composable
private fun NoMatches() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No encontramos esa provincia.\nPrueba con menos letras.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StepProvincePreview() {
    BaliTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            StepProvince(selectedProvince = "Almería", onSelect = {})
        }
    }
}
