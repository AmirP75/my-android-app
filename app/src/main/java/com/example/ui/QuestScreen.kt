package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.data.Quest
import com.example.data.ThemeUnlock
import com.example.data.UserProfile
import com.example.ui.theme.RpgThemeStyle
import com.example.ui.theme.ThemePresets

val LocalLanguage = compositionLocalOf { "FA" }

@Composable
fun str(fa: String, en: String): String {
    return if (LocalLanguage.current == "EN") en else fa
}

@Composable
fun QuestScreen(
    viewModel: QuestViewModel,
    modifier: Modifier = Modifier
) {
    val quests by viewModel.quests.collectAsStateWithLifecycle()
    val rawProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val unlockedThemesList by viewModel.themes.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()
    val kingdomBuildings by viewModel.kingdomBuildings.collectAsStateWithLifecycle()

    val profile = rawProfile ?: UserProfile(1, 1, 0, 15, "CLASSIC")
    val rawTheme = ThemePresets.getThemeById(profile.currentTheme, profile.isDarkMode)
    val activeTheme = com.example.ui.theme.animateRpgThemeAsState(rawTheme)

    val view = androidx.compose.ui.platform.LocalView.current
    var previousLevel by remember { mutableStateOf(profile.level) }
    LaunchedEffect(profile.level) {
        if (profile.level > previousLevel) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            SoundManager.playSound(SoundType.LEVEL_UP)
        }
        previousLevel = profile.level
    }

    var currentTab by remember { mutableStateOf("QUESTS") } // QUESTS, MARKET, KINGDOM, WHEEL
    var showAddQuestDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val bgBrush = if (showAddQuestDialog || showSettingsDialog) {
        androidx.compose.ui.graphics.Brush.linearGradient(activeTheme.gradientColors)
    } else {
        animatedLiquidBackground(activeTheme.gradientColors)
    }

    val layoutDirection = if (profile.language == "FA") androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLanguage provides profile.language,
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgBrush)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
        ) {
            // HERO PROFILE HEADER CARD
            HeroHeaderCard(
                profile = profile,
                activeTheme = activeTheme,
                currentTab = currentTab,
                onTabSelect = { currentTab = it },
                onOpenSettings = { showSettingsDialog = true }
            )

            // MAIN CONTENT PORTAL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                        "QUESTS" -> {
                            QuestBoardSection(
                                quests = quests.filter { !it.isCompleted },
                                activeTheme = activeTheme,
                                onToggleQuest = { 
                                    if (!it.isCompleted) {
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                        SoundManager.playSound(SoundType.COMPLETE_QUEST)
                                    }
                                    if (it.isBoss) viewModel.attackBoss(it) else viewModel.toggleQuestCompletion(it) 
                                },
                                onDeleteQuest = { viewModel.deleteQuest(it) },
                                onAddNewClick = { showAddQuestDialog = true }
                            )
                        }
                        "KINGDOM" -> {
                            KingdomTab(
                                profile = profile,
                                kingdomBuildings = kingdomBuildings,
                                activeTheme = activeTheme,
                                onBuild = { viewModel.buildKingdomBuilding(it) }
                            )
                        }
                        "WHEEL" -> {
                            LootWheelTab(
                                profile = profile,
                                inventoryItems = inventoryItems,
                                activeTheme = activeTheme,
                                onSpinComplete = { p, a -> viewModel.applyLootReward(p, a) }
                            )
                        }
                        "MARKET" -> {
                            ThemeMarketSection(
                                isDarkMode = profile.isDarkMode,
                                activeTheme = activeTheme,
                                currentCoins = profile.coins,
                                unlockedThemes = unlockedThemesList,
                                onSelectTheme = { viewModel.selectTheme(it) },
                                onUnlockTheme = { themeId, cost -> viewModel.unlockTheme(themeId, cost) }
                            )
                        }
                    }
                }
            }

            // STATIC FOOTER SIGNATURE OR HELPER ADVICE
            RpgMiniFooter(activeTheme = activeTheme)
        }

        // FLOATING ACTION BUTTON - Styled in 3D tactile theme
        if (currentTab == "QUESTS") {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 24.dp)
            ) {
                Tactile3DFloatingActionButton(
                    activeTheme = activeTheme,
                    onClick = { showAddQuestDialog = true }
                )
            }
        }
        
        // SETTINGS FLOATING BUTTON
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 24.dp, start = 24.dp)
        ) {
            FloatingActionButton(
                onClick = { showSettingsDialog = true },
                containerColor = activeTheme.surfaceColor,
                contentColor = activeTheme.primaryColor
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings / تنظیمات")
            }
        }

        // ADD QUEST IMMERSIVE DIALOG
        if (showAddQuestDialog) {
            AddQuestDialog(
                activeTheme = activeTheme,
                onDismiss = { showAddQuestDialog = false },
                onConfirm = { title, desc, type, diff, category, isBoss, reminderTime, reminderInterval ->
                    viewModel.addQuest(title, desc, type, category, diff, isBoss, reminderTime, reminderInterval)
                    showAddQuestDialog = false
                }
            )
        }
        
        // CLASS SELECTION DIALOG
        if (profile.characterClass == "UNSELECTED") {
            ClassSelectionDialog(
                activeTheme = activeTheme,
                onSelectClass = { charClass ->
                    viewModel.selectCharacterClass(charClass)
                }
            )
        } else if (!profile.hasSeenOnboarding) {
            OnboardingScreen(
                profile = profile,
                activeTheme = activeTheme,
                onComplete = { viewModel.completeOnboarding() }
            )
        }
        
        if (showSettingsDialog) {
            SettingsDialog(
                profile = profile,
                activeTheme = activeTheme,
                onDismiss = { showSettingsDialog = false },
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onShowOnboarding = {
                    showSettingsDialog = false
                    // Reset onboarding state visually or actually... wait, let's just use a separate state variable for showing onboarding from settings
                }
            )
        }

        if (viewModel.showDailyRewardDialog) {
            DailyRewardDialog(
                profile = profile,
                activeTheme = activeTheme,
                onClaim = { viewModel.claimDailyReward() },
                onDismiss = { viewModel.dismissDailyRewardDialog() }
            )
        }
    }
    }
}

// -------------------------------------------------------------
// COMPONENT: HERO HEADER CARD
// -------------------------------------------------------------
@Composable
fun HeroHeaderCard(
    profile: UserProfile,
    activeTheme: RpgThemeStyle,
    currentTab: String,
    onTabSelect: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val xpNeeded = profile.level * 100
    val xpProgress = (profile.xp.toFloat() / xpNeeded).coerceIn(0f, 1f)

    val (titleRank, titleEmoji) = when (profile.level) {
        in 1..5 -> str("ماجراجوی نوپا", "Novice Adventurer") to "⛺"
        in 6..10 -> str("شکارچی کوئست", "Quest Hunter") to "🏹"
        in 11..20 -> str("قهرمان افسانه‌ای", "Legendary Hero") to "🛡️"
        else -> str("ارباب سیاهچال", "Dungeon Master") to "👑"
    }

    val classIcon = when (profile.characterClass) {
        "WARRIOR" -> "⚔️"
        "MAGE" -> "📚"
        "ROGUE" -> "🎨"
        else -> activeTheme.iconEmoji
    }

    val className = when (profile.characterClass) {
        "WARRIOR" -> str("جنگجو", "Warrior")
        "MAGE" -> str("جادوگر", "Mage")
        "ROGUE" -> str("رند", "Rogue")
        else -> str("قهرمان", "Hero")
    }

    val petIcon = when (profile.petLevel) {
        1 -> "🥚"
        2 -> "🐣"
        3 -> "🐥"
        4 -> "🦅"
        else -> "🐉"
    }

    val daysSinceLastActive = (System.currentTimeMillis() - profile.lastActiveDate) / (1000 * 60 * 60 * 24)
    val petState = if (daysSinceLastActive > 2) str("دلتنگ 🥺", "Lonely 🥺") else petIcon

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .glassmorphism(
                cornerRadius = 32.dp,
                fillAlpha = 0.15f,
                surfaceColor = Color.White,
                borderAlpha = 0.4f
            )
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top row: Avatar & Profile Info + Coins
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Minimal Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .glassmorphism(cornerRadius = 28.dp, fillAlpha = 0.25f, borderAlpha = 0.5f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = classIcon, fontSize = 28.sp)
                }

                // Name & Badges
                Column {
                    Text(
                        text = "$className $titleEmoji",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha=0.3f), androidx.compose.ui.geometry.Offset(1f, 1f), 3f))
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = str("سطح ${profile.level}", "Lv.${profile.level}"),
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha=0.3f), androidx.compose.ui.geometry.Offset(1f, 1f), 3f))
                        )
                        if (profile.streakDays > 0) {
                            Text("🔥 ${profile.streakDays}", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Minimal Coin Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .glassmorphism(cornerRadius = 16.dp, fillAlpha = 0.2f, surfaceColor = Color(0xFFFFD700), borderAlpha = 0.4f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = "🪙", fontSize = 16.sp)
                Text(
                    text = "${profile.coins}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha=0.3f), androidx.compose.ui.geometry.Offset(1f, 1f), 3f))
                )
            }
        }

        // Minimal Experience Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = str("تراز تجربه", "XP Progress"),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha=0.3f), androidx.compose.ui.geometry.Offset(1f, 1f), 3f))
                )
                Text(
                    text = "${profile.xp} / $xpNeeded",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha=0.3f), androidx.compose.ui.geometry.Offset(1f, 1f), 3f))
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .glassmorphism(cornerRadius = 4.dp, fillAlpha = 0.15f, borderAlpha = 0.3f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(xpProgress)
                        .glassmorphism(cornerRadius = 4.dp, fillAlpha = 0.8f, surfaceColor = activeTheme.primaryColor, borderAlpha = 0.5f)
                )
            }
        }

        // Minimal Embedded Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphism(cornerRadius = 16.dp, fillAlpha = 0.1f, borderAlpha = 0.3f)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabSelectorButton(
                title = str("⚔️ ماموریت‌ها", "⚔️ Quests"),
                isSelected = currentTab == "QUESTS",
                activeTheme = activeTheme,
                onClick = { onTabSelect("QUESTS") },
                modifier = Modifier.weight(1f)
            )
            TabSelectorButton(
                title = str("🏰 قلمرو", "🏰 Kingdom"),
                isSelected = currentTab == "KINGDOM",
                activeTheme = activeTheme,
                onClick = { onTabSelect("KINGDOM") },
                modifier = Modifier.weight(1f)
            )
            TabSelectorButton(
                title = str("✨ لوت", "✨ Loot"),
                isSelected = currentTab == "WHEEL",
                activeTheme = activeTheme,
                onClick = { onTabSelect("WHEEL") },
                modifier = Modifier.weight(1f)
            )
            TabSelectorButton(
                title = str("🛒 بازار", "🛒 Market"),
                isSelected = currentTab == "MARKET",
                activeTheme = activeTheme,
                onClick = { onTabSelect("MARKET") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatColumn(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(text = value.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun TabSelectorButton(
    title: String,
    isSelected: Boolean,
    activeTheme: RpgThemeStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (isSelected) activeTheme.secondaryColor else Color.Transparent
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) activeTheme.fontColor else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

// -------------------------------------------------------------
// SECTION 1: QUEST BOARD SECTION (Quests listing, filters, add)
// -------------------------------------------------------------
@Composable
fun QuestBoardSection(
    quests: List<Quest>,
    activeTheme: RpgThemeStyle,
    onToggleQuest: (Quest) -> Unit,
    onDeleteQuest: (Int) -> Unit,
    onAddNewClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, MAIN, SIDE

    val filteredQuests = when (selectedFilter) {
        "ALL" -> quests
        "MAIN" -> quests.filter { it.questType == "MAIN" }
        "SIDE" -> quests.filter { it.questType == "SIDE" }
        else -> quests
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Horizontal filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChipStyled(
                label = str("همه ماموریت‌ها", "All Quests"),
                isSelected = selectedFilter == "ALL",
                activeTheme = activeTheme,
                onClick = { selectedFilter = "ALL" }
            )
            FilterChipStyled(
                label = str("اصلی ⚔️", "Main ⚔️"),
                isSelected = selectedFilter == "MAIN",
                activeTheme = activeTheme,
                onClick = { selectedFilter = "MAIN" }
            )
            FilterChipStyled(
                label = str("فرعی 🎯", "Side 🎯"),
                isSelected = selectedFilter == "SIDE",
                activeTheme = activeTheme,
                onClick = { selectedFilter = "SIDE" }
            )
        }

        // Quests List view
        if (quests.isEmpty()) {
            // RPG Empty state hint
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp)
                        .glassmorphism(cornerRadius = 20.dp, surfaceColor = activeTheme.surfaceColor)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = str("⚔️ دفترچه خالی است! ⚔️", "⚔️ Your Log is Empty! ⚔️"),
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.primaryColor,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = str("قهرمان، هنوز هیچ ماموریت یا کوئستی را در دفترچه رزمی خود ثبت نکرده‌اید!\nهمین حالا با زدن دکمه افزودن (+) اولین ماموریت زندگی خود را آغاز کنید.", "Hero, you haven't written any quests in your combat log!\nStart your first adventure by pressing the Add (+) button."),
                            color = activeTheme.secondaryFontColor,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onAddNewClick,
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor)
                        ) {
                            Text(str("آغاز اولین ماجراجویی", "Start First Adventure"), color = activeTheme.backgroundColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (filteredQuests.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = str("هیچ موردی در این دسته وجود ندارد! 🛡️", "No items in this category! 🛡️"),
                    color = activeTheme.secondaryFontColor,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Daily World Event
                if (selectedFilter == "ALL" || selectedFilter == "MAIN") {
                    item {
                        DailyWorldEventCard(activeTheme = activeTheme)
                    }
                }
                
                items(filteredQuests, key = { it.id }) { quest ->
                    QuestCardItem(
                        quest = quest,
                        activeTheme = activeTheme,
                        onToggle = { onToggleQuest(quest) },
                        onDelete = { onDeleteQuest(quest.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipStyled(
    label: String,
    isSelected: Boolean,
    activeTheme: RpgThemeStyle,
    onClick: () -> Unit
) {
    val labelColor = if (isSelected) Color.White else activeTheme.fontColor
    val chipBg = if (isSelected) activeTheme.primaryColor else Color(0xFFEADDFF).copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(chipBg)
            .border(
                width = 1.3.dp,
                color = if (isSelected) Color.Transparent else activeTheme.primaryColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = labelColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

// -------------------------------------------------------------
// COMPONENT: QUEST CARD ITEM (WITH 3D RPG STYLING)
// -------------------------------------------------------------
@Composable
fun QuestCardItem(
    quest: Quest,
    activeTheme: RpgThemeStyle,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isMain = quest.questType == "MAIN"
    val isCompleted = quest.isCompleted
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showParticles by remember { mutableStateOf(false) }

    val handleToggle: () -> Unit = {
        if (!isCompleted) {
            showParticles = true
            coroutineScope.launch {
                delay(500)
                showParticles = false
                onToggle()
            }
        } else {
            onToggle()
        }
    }

    val difficultyLabel = when (quest.difficulty) {
        "EASY" -> str("آسان ⭐", "Easy ⭐")
        "MEDIUM" -> str("متوسط ⭐⭐", "Medium ⭐⭐")
        "HARD" -> str("سخت ⭐⭐⭐", "Hard ⭐⭐⭐")
        else -> ""
    }

    val diffColor = when (quest.difficulty) {
        "EASY" -> Color(0xFF2E7D32)
        "MEDIUM" -> Color(0xFFEF6C00)
        "HARD" -> Color(0xFFC62828)
        else -> activeTheme.fontColor
    }

    if (isMain && !isCompleted) {
        // GLASSMORPHIC CARD FOR ACTIVE MAIN QUESTS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp, end = 6.dp)
                .glassmorphism(cornerRadius = 24.dp, surfaceColor = activeTheme.surfaceColor)
                .testTag("task_item_card")
        ) {
            // Main foreground card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tactile Checkbox Checklist or Attack Boss button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(if (quest.isBoss) Color(0xFFB71C1C) else Color(0xFFEADDFF), RoundedCornerShape(10.dp))
                            .border(width = 2.dp, color = if (quest.isBoss) Color.Red else activeTheme.primaryColor, shape = RoundedCornerShape(10.dp))
                            .clickable(onClick = handleToggle),
                        contentAlignment = Alignment.Center
                    ) {
                        if (quest.isBoss) {
                            Text("⚔️", fontSize = 16.sp)
                        }
                        if (showParticles) {
                            ParticleBurst()
                        }
                    }

                    // Content text
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Badge(containerColor = activeTheme.primaryColor) {
                                Text(
                                    text = if (quest.isBoss) str("باس بتل 🐉", "Boss Battle 🐉") else str("ماموریت اصلی ⚔️", "Main Quest ⚔️"),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = difficultyLabel,
                                color = diffColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = quest.title,
                            color = activeTheme.fontColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (quest.isBoss) {
                            // Boss HP progress bar
                            val hpRatio = (quest.currentBossHp.toFloat() / quest.maxBossHp.coerceAtLeast(1)).coerceIn(0f, 1f)
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(hpRatio)
                                        .background(Color.Red, RoundedCornerShape(3.dp))
                                )
                            }
                            Text(
                                text = "HP: ${quest.currentBossHp}/${quest.maxBossHp}",
                                color = Color.Red,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (quest.description.isNotEmpty()) {
                            Text(
                                text = quest.description,
                                color = activeTheme.secondaryFontColor,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth(),
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Badge(containerColor = Color(0xFFFFF9C4)) {
                                Text(
                                    text = str("🪙 +${quest.coinsReward} سکه", "🪙 +${quest.coinsReward} Coins"),
                                    color = Color(0xFF5C3C00),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                            Badge(containerColor = Color(0xFFE0F7FA)) {
                                Text(
                                    text = "⭐ +${quest.xpReward} XP",
                                    color = Color(0xFF006064),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = str("حذف کوئست", "Delete Quest"),
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    } else {
        // COMPLETED OR SIDE QUESTS - GLASSY layout
        val fillMod = if (isCompleted) 0.1f else 0.2f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .glassmorphism(cornerRadius = 24.dp, surfaceColor = activeTheme.surfaceColor, fillAlpha = fillMod)
                .padding(16.dp)
                .testTag("task_item_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tactile checkbox status
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            color = if (isCompleted) activeTheme.primaryColor.copy(alpha = 0.15f) else Color.White,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (isCompleted) activeTheme.primaryColor else Color(0xFFCAC4D0),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable(onClick = handleToggle),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = str("تکمیل شده", "Completed"),
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (showParticles) {
                        ParticleBurst()
                    }
                }

                // Details Text
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Badge(containerColor = if (isCompleted) Color(0xFFEADDFF).copy(alpha = 0.5f) else Color(0xFFEADDFF)) {
                            Text(
                                text = if (isCompleted) str("کامل شده", "Completed") else str("ماموریت فرعی 🎯", "Side Quest 🎯"),
                                color = if (isCompleted) activeTheme.secondaryFontColor else activeTheme.primaryColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        if (!isCompleted) {
                            Text(
                                text = difficultyLabel,
                                color = diffColor.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = quest.title,
                        color = if (isCompleted) activeTheme.fontColor.copy(alpha = 0.5f) else activeTheme.fontColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (quest.description.isNotEmpty() && !isCompleted) {
                        Text(
                            text = quest.description,
                            color = activeTheme.secondaryFontColor.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (!isCompleted) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = str("🪙 +${quest.coinsReward} سکه", "🪙 +${quest.coinsReward} Coins"),
                                color = Color(0xFF7A643F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "⭐ +${quest.xpReward} XP",
                                color = Color(0xFF3B6E67),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = str("حذف کوئست", "Delete Quest"),
                        tint = Color(0xFFEF9A9A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClassSelectionDialog(
    activeTheme: RpgThemeStyle,
    onSelectClass: (String) -> Unit
) {
    Dialog(onDismissRequest = { /* forced choice */ }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .glassmorphism(cornerRadius = 24.dp, fillAlpha = 0.98f, surfaceColor = activeTheme.surfaceColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = str("کلاس قهرمان خود را انتخاب کنید!", "Choose your Hero Class!"),
                    color = activeTheme.primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                
                ClassSelectionCard(
                    title = str("جنگجو ⚔️", "Warrior ⚔️"),
                    desc = str("بونوس تجربه برای کوئست‌های جسمی و ورزشی.", "XP bonus for physical & sports quests."),
                    activeTheme = activeTheme,
                    onClick = { onSelectClass("WARRIOR") }
                )
                ClassSelectionCard(
                    title = str("جادوگر 📚", "Mage 📚"),
                    desc = str("بونوس تجربه برای کوئست‌های ذهنی و مطالعه.", "XP bonus for mental & reading quests."),
                    activeTheme = activeTheme,
                    onClick = { onSelectClass("MAGE") }
                )
                ClassSelectionCard(
                    title = str("رند 🎨", "Rogue 🎨"),
                    desc = str("بونوس تجربه برای کارهای خلاقانه و روزمره.", "XP bonus for creative & daily tasks."),
                    activeTheme = activeTheme,
                    onClick = { onSelectClass("ROGUE") }
                )
            }
        }
    }
}

@Composable
fun ClassSelectionCard(title: String, desc: String, activeTheme: RpgThemeStyle, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = activeTheme.fontColor, fontSize = 16.sp)
            Text(desc, color = activeTheme.secondaryFontColor, fontSize = 12.sp)
        }
    }
}

// -------------------------------------------------------------
// SECTION 2: THEME MARKET SECTION (SHOP FOR REWARDS)
// -------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThemeMarketSection(
    isDarkMode: Boolean,
    activeTheme: RpgThemeStyle,
    currentCoins: Int,
    unlockedThemes: List<ThemeUnlock>,
    onSelectTheme: (String) -> Unit,
    onUnlockTheme: (String, Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Bazar header
        Text(
            text = str("🛒 مغازه تم‌های جادویی", "🛒 Magical Themes Shop"),
            color = activeTheme.primaryColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        Text(
            text = str("تم کاربری مورد علاقه را باز کنید و فضای بازی را دگرگون سازید!", "Unlock your favorite UI theme and transform the game!"),
            color = activeTheme.secondaryFontColor,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // List themes grid scroll
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(ThemePresets.getAllThemes(isDarkMode), key = { it.themeId }) { itemTheme ->
                val isUnlocked = unlockedThemes.any { it.themeId == itemTheme.themeId && it.isUnlocked } || itemTheme.isFree
                val isActiveMode = activeTheme.themeId == itemTheme.themeId

                ThemeShopItemRow(
                    themeStyle = itemTheme,
                    isUnlocked = isUnlocked,
                    isActive = isActiveMode,
                    activeTheme = activeTheme,
                    currentCoins = currentCoins,
                    onSelect = { onSelectTheme(itemTheme.themeId) },
                    onUnlock = { onUnlockTheme(itemTheme.themeId, itemTheme.cost) }
                )
            }
        }
    }
}

@Composable
fun ThemeShopItemRow(
    themeStyle: RpgThemeStyle,
    isUnlocked: Boolean,
    isActive: Boolean,
    activeTheme: RpgThemeStyle,
    currentCoins: Int,
    onSelect: () -> Unit,
    onUnlock: () -> Unit
) {
    val canAfford = currentCoins >= themeStyle.cost

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism(cornerRadius = 20.dp, surfaceColor = activeTheme.surfaceColor)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) activeTheme.primaryColor else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Theme Emoji Sphere
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(themeStyle.primaryColor.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
                    .border(width = 1.5.dp, color = themeStyle.primaryColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = themeStyle.iconEmoji,
                    fontSize = 28.sp
                )
            }

            // Description block Farsi
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = themeStyle.titlePersian,
                    fontWeight = FontWeight.Bold,
                    color = activeTheme.fontColor,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = themeStyle.description,
                    color = activeTheme.secondaryFontColor,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 16.sp
                )

                // Cost display
                if (!isUnlocked) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = str("بهای باز شدن: ", "Unlock Cost: "),
                            color = activeTheme.secondaryFontColor,
                            fontSize = 11.sp
                        )
                        Text(
                            text = str("🪙 ${themeStyle.cost} سکه", "🪙 ${themeStyle.cost} Coins"),
                            color = if (canAfford) Color(0xFFFFEA79) else Color(0xFFEF5350),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Action Button Column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isActive) {
                    Badge(
                        containerColor = activeTheme.primaryColor,
                    ) {
                        Text(
                            text = str("فعال", "Active"),
                            fontWeight = FontWeight.Bold,
                            color = activeTheme.backgroundColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp
                        )
                    }
                } else if (isUnlocked) {
                    Button(
                        onClick = onSelect,
                        colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(str("استفاده 🎨", "Use Theme 🎨"), color = activeTheme.backgroundColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = onUnlock,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            disabledContainerColor = Color(0xFF424242)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = str("خرید 🛒", "Buy 🛒"),
                            color = if (canAfford) Color.Black else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// COMPONENT: TACTILE RPG 3D FLOATING ACTION BUTTON
// -------------------------------------------------------------
@Composable
fun Tactile3DFloatingActionButton(
    activeTheme: RpgThemeStyle,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val yOffset by animateDpAsState(targetValue = if (isPressed) 2.dp else 0.dp)

    Box(
        modifier = Modifier
            .offset(y = yOffset)
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = RoundedCornerShape(18.dp)
            )
            .background(
                color = activeTheme.primaryColor,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 3.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                onClick = onClick,
                onClickLabel = str("افزودن کوئست", "Add Quest")
            )
            .padding(16.dp)
            .testTag("submit_button")
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = str("ثبت ماموریت جدید", "Register new mission"),
            tint = activeTheme.backgroundColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

// -------------------------------------------------------------
// COMPONENT: IMMERSIVE RPG DIALOG (ADD QUEST)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestDialog(
    activeTheme: RpgThemeStyle,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, type: String, difficulty: String, category: String, isBoss: Boolean, reminderTime: Long?, reminderInterval: Int?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("MAIN") } // MAIN, SIDE
    var difficulty by remember { mutableStateOf("EASY") } // EASY, MEDIUM, HARD
    var category by remember { mutableStateOf("DAILY") } // PHYSICAL, MENTAL, CREATIVE, DAILY
    var isBoss by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf(false) }

    var hasReminder by remember { mutableStateOf(false) }
    var reminderTimeMillis by remember { mutableStateOf<Long?>(null) }
    var reminderInterval by remember { mutableStateOf(0) } // 0 means none, else minutes

    val context = androidx.compose.ui.platform.LocalContext.current

    val calendar = remember { java.util.Calendar.getInstance() }
    val timePickerDialog = remember {
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute -> 
                calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(java.util.Calendar.MINUTE, minute)
                calendar.set(java.util.Calendar.SECOND, 0)
                if (calendar.timeInMillis < System.currentTimeMillis()) {
                    // We don't want to auto-add 1 day if they specifically set a date today in the past, but keep it simple, or warn. Let's just set the time.
                    // Instead of adding a day automatically (since we have date picker now), we just set the time.
                }
                reminderTimeMillis = calendar.timeInMillis
            },
            calendar.get(java.util.Calendar.HOUR_OF_DAY),
            calendar.get(java.util.Calendar.MINUTE),
            true
        )
    }

    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(java.util.Calendar.YEAR, year)
                calendar.set(java.util.Calendar.MONTH, month)
                calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                timePickerDialog.show()
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .glassmorphism(cornerRadius = 24.dp, fillAlpha = 0.98f, surfaceColor = activeTheme.surfaceColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = str("📜 کتیبه ماموریت جدید", "📜 New Quest Scroll"),
                    color = activeTheme.primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Error message Farsi
                if (validationError) {
                    Text(
                        text = str("لطفا عنوان ماموریت را وارد کنید!", "Please enter the quest title!"),
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Title Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(str("عنوان ماموریت شما:", "Quest Title:"), color = activeTheme.fontColor, fontSize = 12.sp)
                    TextField(
                        value = title,
                        onValueChange = {
                            title = it
                            validationError = false
                        },
                        placeholder = { Text(str("مثلاً: غول پایگاه اول (ورزش روزانه)", "e.g., First Base Boss (Daily exercise)"), fontSize = 12.sp, color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().testTag("username_input"),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.03f),
                            focusedTextColor = activeTheme.fontColor,
                            unfocusedTextColor = activeTheme.fontColor,
                            cursorColor = activeTheme.primaryColor
                        )
                    )
                }

                // Description Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(str("شرح ماموریت کوئست (اختیاری):", "Quest Description (Optional):"), color = activeTheme.fontColor, fontSize = 12.sp)
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text(str("انجام کارهای جزیی تر ماموریت...", "Sub-tasks or details..."), fontSize = 12.sp, color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.03f),
                            focusedTextColor = activeTheme.fontColor,
                            unfocusedTextColor = activeTheme.fontColor,
                            cursorColor = activeTheme.primaryColor
                        )
                    )
                }

                // Type Choice (Main Quest vs Side Quest)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(str("دسته بندی ماموریت (اصلی/فرعی):", "Quest Type (Main/Side):"), color = activeTheme.fontColor, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { type = "MAIN" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "MAIN") activeTheme.primaryColor else Color.Transparent
                            ),
                            elevation = null,
                            border = BorderStroke(1.dp, activeTheme.primaryColor)
                        ) {
                            Text(
                                str("اصلی ⚔️", "Main ⚔️"),
                                color = if (type == "MAIN") activeTheme.backgroundColor else activeTheme.primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { type = "SIDE" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "SIDE") activeTheme.secondaryColor else Color.Transparent
                            ),
                            elevation = null,
                            border = BorderStroke(1.dp, activeTheme.secondaryColor)
                        ) {
                            Text(
                                str("فرعی 🎯", "Side 🎯"),
                                color = if (type == "SIDE") activeTheme.backgroundColor else activeTheme.secondaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Category selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(str("کلاس ماموریت (بونوس استتات):", "Quest Class (Stat Bonus):"), color = activeTheme.fontColor, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val categories = if (LocalLanguage.current == "EN") {
                            listOf("PHYSICAL" to "Physical 💪", "MENTAL" to "Mental 🧠", "CREATIVE" to "Creative🎨", "DAILY" to "Daily📋")
                        } else {
                            listOf("PHYSICAL" to "جسمی 💪", "MENTAL" to "ذهنی 🧠", "CREATIVE" to "خلاق🎨", "DAILY" to "روزمره📋")
                        }
                        categories.forEach { (catType, catLabel) ->
                            val isSel = catType == category
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) activeTheme.primaryColor else Color.Black.copy(alpha = 0.15f))
                                    .clickable { category = catType }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(catLabel, color = if (isSel) Color.White else activeTheme.fontColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Difficulty selector (Easy, Medium, Hard)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(str("سختی نبرد و غنائم ارسالی:", "Battle Difficulty & Loot:"), color = activeTheme.fontColor, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("EASY", "MEDIUM", "HARD").forEach { diffLevel ->
                            val txt = when(diffLevel) {
                                "EASY" -> str("آسان 🪙5", "Easy 🪙5")
                                "MEDIUM" -> str("متوسط 🪙12", "Med 🪙12")
                                "HARD" -> str("سخت 🪙25", "Hard 🪙25")
                                else -> ""
                            }
                            val isSelected = difficulty == diffLevel
                            val activeBg = when(diffLevel) {
                                "EASY" -> Color(0xFF2E7D32)
                                "MEDIUM" -> Color(0xFFEF6C00)
                                "HARD" -> Color(0xFFC62828)
                                else -> activeTheme.primaryColor
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) activeBg else Color.Black.copy(alpha = 0.25f))
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { difficulty = diffLevel }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = txt,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Reminder
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(checked = hasReminder, onCheckedChange = { hasReminder = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(str("یادآوری (نوتیفیکیشن)", "Reminder (Notification)"), color = activeTheme.fontColor, fontSize = 12.sp)
                }

                if (hasReminder) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { datePickerDialog.show() },
                            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor)
                        ) {
                            Text(str("تنظیم زمان بازگشت", "Set Time/Date"), fontSize = 12.sp, color = activeTheme.backgroundColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (reminderTimeMillis != null) str("تنظیم شد ✅", "Set ✅") else str("زمان هنوز تنظیم نشده", "Not set"),
                            color = activeTheme.fontColor, fontSize = 12.sp
                        )
                    }
                    
                    Text(str("در صورت انجام ندادن، تکرار شود پس از:", "If not completed, repeat after:"), color = activeTheme.fontColor, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0 to "بدون تکرار", 5 to "۵ دقیقه", 15 to "۱۵ دقیقه", 60 to "۱ ساعت").forEach { (min, label) ->
                            val sel = reminderInterval == min
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) activeTheme.primaryColor else Color.Black.copy(alpha=0.25f))
                                    .border(1.dp, if (sel) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable{ reminderInterval = min }.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(if (com.example.ui.LocalLanguage.current == "FA") label else if (min == 0) "None" else "$min min", fontSize = 10.sp, color=Color.White, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                // Boss switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(checked = isBoss, onCheckedChange = { isBoss = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(str("تبدیل به باس بتل بزرگ (با نوار سلامت)", "Turn into a major Boss Battle (with HP bar)"), color = activeTheme.fontColor, fontSize = 12.sp)
                }

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(str("انصراف", "Cancel"), color = activeTheme.fontColor)
                    }

                    Button(
                        onClick = {
                            if (title.trim().isEmpty()) {
                                validationError = true
                            } else {
                                val rTime = if (hasReminder) reminderTimeMillis else null
                                val rInterval = if (hasReminder && reminderInterval > 0) reminderInterval else null
                                onConfirm(title, description, type, difficulty, category, isBoss, rTime, rInterval)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(str("ایجاد کوئست 🗡️", "Create Quest 🗡️"), color = activeTheme.backgroundColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RpgMiniFooter(activeTheme: RpgThemeStyle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.5f))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = str("دفتر ثبت ماجراجویی‌های روزانه (QuestLog) - نسخه نهایی 🛡️🏆", "Daily Adventure Log (QuestLog) - Final Edition 🛡️🏆"),
                color = activeTheme.secondaryFontColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
