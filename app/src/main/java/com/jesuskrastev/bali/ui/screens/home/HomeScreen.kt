package com.jesuskrastev.bali.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jesuskrastev.bali.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: HomeViewModel,
    onStudyClick: () -> Unit = {},
    onTopicsClick: () -> Unit = {},
    onMistakesClick: () -> Unit = {},
    onExamClick: () -> Unit = {},
    onShopClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onAuthClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLevelLockedDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showEnergyBottomSheet by remember { mutableStateOf(false) }
    var showMonthlyStreakCalendar by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
            ) {
                Spacer(Modifier.height(48.dp))
                
                // Profile Header Section
                if (uiState.isLoggedIn) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.profilePictureUrl != null) {
                            AsyncImage(
                                model = uiState.profilePictureUrl,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = uiState.userName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            
                            Text(
                                text = uiState.userEmail ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // "Sign In" Section (Style from image)
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .fillMaxWidth()
                            .clickable { 
                                scope.launch { drawerState.close() }
                                onAuthClick()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                                        append("Regístrate")
                                    }
                                    append(" o ")
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                                        append("inicia sesión")
                                    }
                                    append(" si ya")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "tienes una cuenta",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Navigation Items
                NavigationDrawerItem(
                    label = { Text("Notificaciones") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Rounded.Notifications, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Política de privacidad") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://baliaipage.vercel.app/privacidad.html"))
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Rounded.Security, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Términos y condiciones") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://baliaipage.vercel.app/terminos.html"))
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Rounded.Description, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Enviar sugerencia") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onFeedbackClick()
                    },
                    icon = { Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(Modifier.weight(1f))

                // Logout
                if (uiState.isLoggedIn) {
                    NavigationDrawerItem(
                        label = { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showLogoutConfirmDialog = true
                        },
                        icon = { Icon(Icons.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    ) {
        // Diálogo de confirmación de cierre de sesión
        if (showLogoutConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirmDialog = false },
                title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold) },
                text = { Text("¿Estás seguro de que quieres cerrar sesión? Tu progreso se sincronizará la próxima vez que entres.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutConfirmDialog = false
                            viewModel.signOut(context)
                        }
                    ) {
                        Text("CERRAR SESIÓN", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirmDialog = false }) {
                        Text("CANCELAR", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        // Diálogo de falta de energía
        if (uiState.showEnergyDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissEnergyDialog() },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.bali_off),
                            contentDescription = null,
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("¡Sin energía!", fontWeight = FontWeight.Black)
                    }
                },
                text = {
                    Text(
                        "Me he quedado sin energía por hoy... Necesito descansar un poco para poder ayudarte mejor mañana.",
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissEnergyDialog() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("ENTENDIDO", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(32.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        // Diálogo de falta de monedas
        if (uiState.showNoCoinsDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissNoCoinsDialog() },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.coin),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("¡Sin monedas!", fontWeight = FontWeight.Black)
                    }
                },
                text = {
                    Text(
                        "Necesitas 100 monedas para realizar un examen oficial. ¡Sigue practicando para ganar más!",
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissNoCoinsDialog() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("ENTENDIDO", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(32.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        // Diálogo de nivel bloqueado
        if (showLevelLockedDialog) {
            AlertDialog(
                onDismissRequest = { showLevelLockedDialog = false },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔒",
                                fontSize = 32.sp,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("¡Nivel insuficiente!", fontWeight = FontWeight.Black)
                    }
                },
                text = {
                    Text(
                        "Todavía no tienes suficiente experiencia para el examen oficial. ¡Sigue practicando hasta llegar al nivel 7!",
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showLevelLockedDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("¡A ENTRENAR!", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(32.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (showMonthlyStreakCalendar) {
            MonthlyStreakCalendarDialog(
                currentStreak = uiState.streak,
                freezersAvailable = uiState.streakFreezes,
                practiceDays = uiState.practiceDays,
                onDismiss = { showMonthlyStreakCalendar = false }
            )
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(
                    modifier = if (uiState.isLoggedIn) Modifier.statusBarsPadding() else Modifier.padding(0.dp)
                ) {
                    AnimatedVisibility(
                        visible = !uiState.isLoggedIn,
                    ) {
                        SyncBanner(
                            onAction = onAuthClick
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        UserStatusRow(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            streak = uiState.streak,
                            energyCount = uiState.energyCount,
                            coinsCount = uiState.coinsCount,
                            onCoinsClick = onShopClick,
                            onEnergyClick = { showEnergyBottomSheet = true },
                            onMenuClick = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1.5. Daily Tip Card
                item {
                    DailyTipCard(tip = uiState.dailyTip)
                }

                // 1.6. Weekly Streak Table
                if (uiState.isLoggedIn) {
                    item {
                        WeeklyStreakTable(
                            weeklyStreak = uiState.weeklyStreak,
                            currentStreak = uiState.streak,
                            freezersAvailable = uiState.streakFreezes,
                            onClick = { showMonthlyStreakCalendar = true }
                        )
                    }
                }

                // 2. Weekly Challenge Card
                item {
                    WeeklyChallengeCard(
                        title = "Reto de Señales",
                        subtitle = "Domina las señales para asegurar tu aprobado.",
                        onStartClick = {
                            if (uiState.energyCount > 0) onStudyClick()
                            else viewModel.showEnergyDialog()
                        }
                    )
                }

                // 3. PREPARATION SECTION
                item {
                    SectionHeader(title = "Tu Preparación")
                }

                item {
                    HomeCard(
                        title = "Test rapido",
                        subtitle = "Sesiones cortas de 10 preguntas",
                        emoji = "⚡",
                        actionText = "Comenzar",
                        onClick = {
                            if (uiState.energyCount > 0) onStudyClick()
                            else viewModel.showEnergyDialog()
                        }
                    )
                }

                item {
                    HomeCard(
                        title = "Tests por tema",
                        subtitle = "Enfócate en lo que más te cuesta",
                        emoji = "📚",
                        actionText = "Explorar",
                        onClick = {
                            onTopicsClick()
                        }
                    )
                }

                if(uiState.mistakesCount > 0) {
                    item {
                        HomeCard(
                            title = "Repasar errores",
                            subtitle = "Aprende de tus últimos fallos",
                            emoji = "❌",
                            actionText = "Repasar",
                            onClick = {
                                if (uiState.energyCount > 0) onMistakesClick()
                                else viewModel.showEnergyDialog()
                            }
                        )
                    }
                }

                item {
                    val isExamLocked = uiState.xpLevel < 7
                    HomeCard(
                        title = "Examen DGT",
                        subtitle = if (isExamLocked) "Desbloquea al nivel 7" else "Simulacro oficial de 30 preguntas",
                        emoji = if (isExamLocked) "🔒" else "🎓",
                        actionText = if (isExamLocked) "Bloqueado" else "100",
                        actionIcon = if (isExamLocked) null else R.drawable.coin,
                        isLocked = isExamLocked,
                        onClick = {
                            if (isExamLocked) {
                                showLevelLockedDialog = true
                            } else if (uiState.energyCount > 0) {
                                viewModel.startExam {
                                    onExamClick()
                                }
                            } else {
                                viewModel.showEnergyDialog()
                            }
                        }
                    )
                }

                // 4. STATISTICS SECTION
                item {
                    SectionHeader(title = "Estadísticas")
                }

                item {
                    QuickStatsRow(
                        streak = uiState.streak,
                        accuracy = uiState.avgScore,
                        totalTests = uiState.totalTests
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
    
    if (showEnergyBottomSheet) {
        EnergyBottomSheet(
            energyCount = uiState.energyCount,
            lastEnergyUpdateTimestamp = uiState.lastEnergyUpdateTimestamp,
            onDismissRequest = { showEnergyBottomSheet = false },
            onGoToShopClick = {
                showEnergyBottomSheet = false
                onShopClick()
            }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EnergyBottomSheet(
    energyCount: Int,
    lastEnergyUpdateTimestamp: Long,
    onDismissRequest: () -> Unit,
    onGoToShopClick: () -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (index < energyCount) Color(0xFFFACC15) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (energyCount >= 5) {
                Text(
                    text = "Al máximo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tu energía está llena. Necesitas energía para continuar tu estudio. Cuando se agote, se rellenará automáticamente.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                var remainingTime by remember { mutableStateOf(0L) }
                
                LaunchedEffect(lastEnergyUpdateTimestamp) {
                    val twoHoursMillis = 2 * 60 * 60 * 1000L
                    while(true) {
                        val currentTime = System.currentTimeMillis()
                        val diff = currentTime - lastEnergyUpdateTimestamp
                        val remainder = diff % twoHoursMillis
                        remainingTime = twoHoursMillis - remainder
                        kotlinx.coroutines.delay(1000)
                    }
                }
                
                val hours = (remainingTime / (1000 * 60 * 60))
                val minutes = (remainingTime / (1000 * 60)) % 60
                val seconds = (remainingTime / 1000) % 60
                val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                
                Text(
                    text = buildAnnotatedString {
                        append("Próxima energía en ")
                        withStyle(style = SpanStyle(color = Color(0xFFEF4444))) {
                            append(timeString)
                        }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Toma un breve descanso para recargar energía, o consigue más en la tienda para seguir aprendiendo.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (energyCount < 5) {
                Button(
                    onClick = onGoToShopClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFACC15), contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ir a la tienda", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salir", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SyncBanner(onAction: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clipToBounds()
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 64.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "¡Inicia sesión para sincronizar tu progreso!",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    onClick = onAction,
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "OK",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Bali mascot peeking diagonally from the corner, smaller size
            Image(
                painter = painterResource(id = R.drawable.bali),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-25).dp, y = 0.dp)
                    .rotate(-5f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun DailyTipCard(tip: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Adorno visual en la esquina
            Canvas(modifier = Modifier.size(80.dp).align(Alignment.BottomEnd).offset(x = 10.dp, y = 10.dp)) {
                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = 0.05f),
                    radius = size.minDimension / 2
                )
            }

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFFFACC15), // Color Bali (Amarillo)
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "CONSEJO DEL DÍA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Bali AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "“$tip”",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PremiumBanner(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit
) {
    val premiumGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFF6B35),
            Color(0xFFFFB037)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = onClick
    ) {
        Box(modifier = Modifier.background(premiumGradient)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = 120.dp.toPx(),
                    center = Offset(size.width * 0.9f, size.height * 0.2f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = 60.dp.toPx(),
                    center = Offset(size.width * 0.1f, size.height * 0.8f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.crown),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier.size(20.dp).rotate(-15f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BALI PREMIUM",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Aprueba a la primera",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp
                    )

                    Text(
                        text = "Tu éxito no puede esperar",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = "LO QUIERO",
                            color = Color(0xFFFF6B35),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                Box(modifier = Modifier.weight(0.8f), contentAlignment = Alignment.CenterEnd) {
                    val infiniteTransition = rememberInfiniteTransition(label = "float")
                    val floatOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -12f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "float"
                    )

                    with(sharedTransitionScope) {
                        Image(
                            painter = painterResource(id = R.drawable.bali),
                            contentDescription = "Mascota Bali Premium",
                            modifier = Modifier
                                .size(120.dp)
                                .offset(y = floatOffset.dp)
                                .sharedElement(
                                    rememberSharedContentState(key = "bali_mascot"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserStatusRow(
    modifier: Modifier = Modifier,
    streak: Int,
    energyCount: Int,
    coinsCount: Int,
    onCoinsClick: () -> Unit = {},
    onEnergyClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "Menú",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Streak
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.streak),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$streak",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Coins with clickable visual cue
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onCoinsClick() },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Image(
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit,
                        painter = painterResource(id = R.drawable.coin),
                        contentDescription = null,
                    )
                    Text(
                        text = "$coinsCount",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Energy with clickable visual cue
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onEnergyClick() },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFACC15),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$energyCount",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyChallengeCard(
    title: String,
    subtitle: String,
    onStartClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1A1A1A)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dotRadius = 1.dp.toPx()
                val spacing = 20.dp.toPx()
                for (x in 0..size.width.toInt() step spacing.toInt()) {
                    for (y in 0..size.height.toInt() step spacing.toInt()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = dotRadius,
                            center = Offset(x.toFloat(), y.toFloat())
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Surface(
                        color = Color(0xFFFACC15),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = "RETO SEMANAL",
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFFACC15).copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔥", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "12k+ Alumnos activos",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Button(
                        onClick = onStartClick,
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFACC15),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFFFACC15).copy(alpha = 0.5f),
                            disabledContentColor = Color.Black.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        Text("Próximamente", fontWeight = FontWeight.Black)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 20.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(60.dp)
                        .offset(x = 0.dp, y = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                Surface(
                    modifier = Modifier
                        .size(45.dp)
                        .offset(x = (-20).dp, y = 40.dp),
                    shape = CircleShape,
                    color = Color(0xFF3B82F6)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Navigation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatsRow(streak: Int, accuracy: Int, totalTests: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatBox(modifier = Modifier.weight(1f), label = "Racha", value = "${streak}d", emoji = "🔥")
        StatBox(modifier = Modifier.weight(1f), label = "Precisión", value = "$accuracy%", emoji = "🎯")
        StatBox(modifier = Modifier.weight(1f), label = "Tests", value = "$totalTests", emoji = "📊")
    }
}

@Composable
fun StatBox(modifier: Modifier, label: String, value: String, emoji: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun HomeCard(
    title: String,
    subtitle: String,
    emoji: String,
    actionText: String,
    actionIcon: Int? = null,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().alpha(if (isLocked) 0.7f else 1f),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant, 
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 24.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp, 
                    color = if (isLocked) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) 
                            else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = actionText,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                    )
                    if (actionIcon != null) {
                        Spacer(Modifier.width(4.dp))
                        Image(
                            painter = painterResource(id = R.drawable.coin),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyStreakTable(
    weeklyStreak: List<DailyStreakState>,
    currentStreak: Int,
    freezersAvailable: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tu Semana",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Ver Calendario",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyStreak.forEach { day ->
                    StreakDayItem(day)
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Racha actual: ${currentStreak} días \uD83D\uDD25",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Congeladores: $freezersAvailable \u2744\uFE0F",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StreakDayItem(day: DailyStreakState) {
    val isToday = day.isToday
    
    val backgroundColor = when (day.status) {
        StreakStatus.COMPLETED -> Color(0xFFFACC15).copy(alpha = 0.2f)
        StreakStatus.FROZEN -> Color(0xFF60A5FA).copy(alpha = 0.2f)
        StreakStatus.FAILED, StreakStatus.FUTURE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when (day.status) {
        StreakStatus.COMPLETED -> Color(0xFFF59E0B)
        StreakStatus.FROZEN -> Color(0xFF60A5FA)
        StreakStatus.FAILED, StreakStatus.FUTURE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val icon = when (day.status) {
        StreakStatus.COMPLETED -> "\uD83D\uDD25"
        StreakStatus.FROZEN -> "\u2744\uFE0F"
        else -> ""
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = day.dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Surface(
            shape = CircleShape,
            color = backgroundColor,
            modifier = Modifier.size(36.dp),
            border = if (isToday) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (icon.isNotEmpty()) {
                    Text(text = icon, fontSize = 16.sp, modifier = Modifier.offset(y = (-1).dp))
                } else {
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyStreakCalendarDialog(
    currentStreak: Int,
    freezersAvailable: Int,
    practiceDays: List<Long>,
    onDismiss: () -> Unit
) {
    var displayMonth by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    val today = java.util.Calendar.getInstance()
    
    // Grid calculations
    val daysInMonth = displayMonth.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = displayMonth.clone() as java.util.Calendar
    firstDayOfMonth.set(java.util.Calendar.DAY_OF_MONTH, 1)
    
    var firstDayOfWeek = firstDayOfMonth.get(java.util.Calendar.DAY_OF_WEEK) - 2
    if (firstDayOfWeek < 0) firstDayOfWeek += 7
    
    val monthName = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("es", "ES")).format(displayMonth.time).replaceFirstChar { it.uppercase() }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Navigation)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        displayMonth = (displayMonth.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -1) } 
                    }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Mes anterior")
                    }
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { 
                        displayMonth = (displayMonth.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, 1) }
                    }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Mes siguiente")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Days of week header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Grid
                val totalCells = firstDayOfWeek + daysInMonth
                val rows = kotlin.math.ceil(totalCells / 7.0).toInt()
                
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (c in 0..6) {
                            val cellIndex = r * 7 + c
                            val dayNumber = cellIndex - firstDayOfWeek + 1
                            
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNumber in 1..daysInMonth) {
                                    val cal = displayMonth.clone() as java.util.Calendar
                                    cal.set(java.util.Calendar.DAY_OF_MONTH, dayNumber)
                                    
                                    val isToday = com.jesuskrastev.bali.domain.util.DateTimeHelper().isSameDay(cal.timeInMillis, today.timeInMillis)
                                    val isFuture = cal.after(today) && !isToday
                                    
                                    val dayStartMillis = cal.timeInMillis
                                    val hasPracticed = practiceDays.contains(dayStartMillis)
                                    
                                    val status = if (isFuture) {
                                        StreakStatus.FUTURE
                                    } else if (isToday) {
                                        if (hasPracticed) StreakStatus.COMPLETED else StreakStatus.TODAY
                                    } else {
                                        if (hasPracticed) {
                                            StreakStatus.COMPLETED
                                        } else {
                                            StreakStatus.FAILED
                                        }
                                    }

                                    val bg = when(status) {
                                        StreakStatus.COMPLETED -> Color(0xFFFACC15).copy(alpha = 0.2f)
                                        StreakStatus.FROZEN -> Color(0xFF60A5FA).copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                    val fg = when(status) {
                                        StreakStatus.COMPLETED -> Color(0xFFF59E0B)
                                        StreakStatus.FROZEN -> Color(0xFF60A5FA)
                                        StreakStatus.FUTURE, StreakStatus.FAILED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                    val icon = when(status) {
                                        StreakStatus.COMPLETED -> "\uD83D\uDD25"
                                        StreakStatus.FROZEN -> "\u2744\uFE0F"
                                        else -> ""
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = bg,
                                        modifier = Modifier.size(36.dp).alpha(if (status == StreakStatus.FUTURE || status == StreakStatus.FAILED) 0.5f else 1f),
                                        border = if (isToday) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (icon.isNotEmpty() && status != StreakStatus.FUTURE && !isFuture) {
                                                Text(text = icon, fontSize = 16.sp, modifier = Modifier.offset(y = (-1).dp))
                                            } else {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = fg
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summary Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Racha \nActual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Text("${currentStreak}r", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mejor \nRacha", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Text("${currentStreak}r", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Congeladores\nDisponibles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Text("$freezersAvailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("CERRAR CALENDARIO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}