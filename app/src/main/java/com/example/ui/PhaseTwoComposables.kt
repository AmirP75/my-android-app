package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.RpgThemeStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DailyRewardDialog(
    profile: UserProfile,
    activeTheme: RpgThemeStyle,
    onClaim: () -> Unit,
    onDismiss: () -> Unit
) {
    val l = profile.language
    val str = { fa: String, en: String -> if (l == "EN") en else fa }
    
    // Add a simple pop-in animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    if (visible) {
        Dialog(onDismissRequest = onDismiss) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .glassmorphism(
                        cornerRadius = 28.dp,
                        fillAlpha = 0.2f,
                        surfaceColor = Color.White,
                        borderAlpha = 0.5f
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Icon / Emoji
                    Text(
                        text = "🎁",
                        fontSize = 56.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha=0.3f), Offset(2f, 2f), 8f)
                        )
                    )
                    
                    Text(
                        text = str("پاداش ورود روزانه!", "Daily Login Reward!"),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha=0.5f), Offset(1f, 1f), 4f)
                        )
                    )
                    
                    Text(
                        text = str("به خاطر ورود روزانه‌ات به برنامه، ۵۰ سکه دریافت کردی!", "You received 50 coins for logging in today!"),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .glassmorphism(cornerRadius = 16.dp, fillAlpha = 0.3f, surfaceColor = Color(0xFFFFD700), borderAlpha = 0.6f)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(text = "🪙", fontSize = 24.sp)
                        Text(
                            text = "+50",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 24.sp,
                            style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha=0.3f), Offset(1f, 1f), 3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onClaim,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .glassmorphism(cornerRadius = 16.dp, fillAlpha = 0.8f, surfaceColor = activeTheme.primaryColor, borderAlpha = 0.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text(
                            text = str("دریافت پاداش", "Claim Reward"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityHeatmap(
    profile: UserProfile,
    activeTheme: RpgThemeStyle
) {
    val random = java.util.Random(profile.id.toLong()) // pseudo-deterministic
    val days = 30 // 30 days history
    val data = List(days) { index ->
        if (index == days - 1) profile.questsCompletedToday
        else if (index > days - profile.streakDays - 1) random.nextInt(4) + 1
        else if (random.nextBoolean()) random.nextInt(3) else 0
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("تقویم فعالیت (۳۰ روز)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            data.forEach { count ->
                val intensity = (count.toFloat() / 5f).coerceIn(0f, 1f)
                val boxColor = if (count == 0) Color.White.copy(alpha = 0.1f) else activeTheme.primaryColor.copy(alpha = 0.3f + (0.7f * intensity))
                Box(
                    modifier = Modifier.padding(1.dp).size(8.dp).clip(RoundedCornerShape(2.dp)).background(boxColor)
                )
            }
        }
    }
}
@Composable
fun DailyWorldEventCard(activeTheme: RpgThemeStyle) {
    val events = listOf(
        "امروز ۸ لیوان آب بنوش!",
        "۱۰ دقیقه مدیتیشن کن و آرام باش.",
        "روی یک صفحه کتاب تمرکز کن و بخوان.",
        "به یک دوست قدیمی پیام بده.",
        "۱۵ دقیقه پیاده‌روی تند داشته باش."
    )
    val calendar = java.util.Calendar.getInstance()
    val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    val eventIndex = dayOfYear % events.size
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .glassmorphism(cornerRadius = 16.dp, surfaceColor = activeTheme.surfaceColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFFFD700).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🌟", fontSize = 20.sp)
            }
            Column {
                Text("رویداد ویژه روز!", color = Color(0xFFFFA000), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(events[eventIndex], color = activeTheme.fontColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SettingsDialog(
    profile: UserProfile,
    activeTheme: RpgThemeStyle,
    onDismiss: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleLanguage: () -> Unit,
    onShowOnboarding: () -> Unit
) {
    val l = profile.language
    val str = { fa: String, en: String -> if (l == "EN") en else fa }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .glassmorphism(cornerRadius = 24.dp, fillAlpha = 0.98f, surfaceColor = activeTheme.surfaceColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(str("تنظیمات", "Settings"), color = activeTheme.primaryColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = str("بستن", "Close"), tint = activeTheme.fontColor)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(str("حالت تاریک", "Dark Mode"), color = activeTheme.fontColor, fontSize = 16.sp)
                    Switch(checked = profile.isDarkMode, onCheckedChange = { onToggleDarkMode() })
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(str("زبان", "Language"), color = activeTheme.fontColor, fontSize = 16.sp)
                    Button(onClick = onToggleLanguage, colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor)) {
                        Text(if (profile.language == "FA") "فارسی" else "English", color = Color.White)
                    }
                }
                
                Button(
                    onClick = onShowOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.secondaryColor)
                ) {
                    Text(str("مشاهده راهنمای بازی", "View Game Guide"), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    profile: UserProfile?, // Passed for language check (optional fallback)
    activeTheme: RpgThemeStyle,
    onComplete: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    val l = profile?.language ?: "FA"
    val str = { fa: String, en: String -> if (l == "EN") en else fa }

    val pages = if (l == "EN") {
        listOf(
            Pair("Welcome to QuestLog!", "This app turns your daily tasks into an engaging RPG adventure. 🗺️"),
            Pair("Quests & Rewards", "Gain Coins and XP by completing main or side quests. 💪🧠🎨"),
            Pair("Shop & Kingdom", "Use coins to unlock new themes or build structures in your Kingdom! 🏰"),
            Pair("Your Pet Companion", "Your pet levels up as you complete quests. Don't let it get lonely! 🐉")
        )
    } else {
        listOf(
            Pair("به QuestLog خوش آمدید!", "این برنامه به شما کمک می‌کند کارهای روزانه‌تان را به یک بازی نقش‌آفرینی جذاب تبدیل کنید. 🗺️"),
            Pair("ماموریت‌ها و پاداش", "با تکمیل هر ماموریت اصلی یا فرعی، سکه و XP دریافت می‌کنید. 💪🧠🎨"),
            Pair("خرید تم و قلمرو", "با سکه‌هایتان قفل تم‌های جدید را در فروشگاه باز کنید یا برای قلمروی خود ساختمان بسازید! 🏰"),
            Pair("حیوان خانگی شما", "پت شما با کامل کردن کوئست‌ها لول‌آپ می‌شود. نگذارید دلتنگ شود! 🐉")
        )
    }

    Dialog(onDismissRequest = { /* force action */ }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .background(activeTheme.surfaceColor, RoundedCornerShape(24.dp))
                .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(pages[currentPage].first, color = activeTheme.primaryColor, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
                Text(pages[currentPage].second, color = activeTheme.fontColor, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.height(80.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (index == currentPage) activeTheme.primaryColor else Color.Gray.copy(alpha = 0.5f))
                        )
                    }
                }

                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) currentPage++ else onComplete()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor)
                ) {
                    Text(if (currentPage < pages.size - 1) str("بعدی", "Next") else str("شروع ماجراجویی!", "Start Adventure!"), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun KingdomTab(
    profile: UserProfile,
    kingdomBuildings: List<KingdomBuilding>,
    activeTheme: RpgThemeStyle,
    onBuild: (String) -> Unit
) {
    val l = profile.language
    val str = { fa: String, en: String -> if (l == "EN") en else fa }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(str("قلمرو شخصی من 🏰", "My Kingdom 🏰"), fontWeight = FontWeight.Bold, color = activeTheme.primaryColor, fontSize = 20.sp)
        Text(str("با خرید ساختمان‌ها قلمرو خود را زیباتر کنید.", "Beautify your realm by building structures."), color = activeTheme.secondaryFontColor, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Profile stats
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatColumn(label = str("قدرت 💪", "Strength 💪"), value = profile.strength)
            StatColumn(label = str("هوش 🧠", "Intelligence 🧠"), value = profile.intelligence)
            StatColumn(label = str("کاریزما 🎨", "Charisma 🎨"), value = profile.charisma)
            StatColumn(label = str("نظم 📋", "Discipline 📋"), value = profile.discipline)
        }

        ActivityHeatmap(profile = profile, activeTheme = activeTheme)

        // Visual Map Area (Isometric Scene upgraded)
        Box(modifier = Modifier.fillMaxWidth().height(350.dp).clip(RoundedCornerShape(16.dp))) {
            IsometricScene(profile, kingdomBuildings, activeTheme)
        }

        // Available Buildings
        Text(str("ساختمان‌های قابل ساخت:", "Available Buildings:"), fontWeight = FontWeight.Bold, color = activeTheme.fontColor, fontSize = 16.sp)
        kingdomBuildings.filter { !it.isBuilt }.forEach { building ->
            Row(
                modifier = Modifier.fillMaxWidth().glassmorphism(cornerRadius = 12.dp, surfaceColor = activeTheme.surfaceColor, fillAlpha = 0.4f).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(building.emoji, fontSize = 30.sp)
                    Column {
                        Text(building.name, color = activeTheme.fontColor, fontWeight = FontWeight.Bold)
                        Text(str("هزینه: ${building.cost} سکه", "Cost: ${building.cost} Coins"), color = activeTheme.secondaryFontColor, fontSize = 12.sp)
                    }
                }
                Button(
                    onClick = { onBuild(building.id) },
                    enabled = profile.coins >= building.cost,
                    colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor)
                ) {
                    Text(str("ساختن", "Build"))
                }
            }
        }
    }
}

@Composable
fun LootWheelTab(
    profile: UserProfile,
    inventoryItems: List<InventoryItem>,
    activeTheme: RpgThemeStyle,
    onSpinComplete: (String, Int) -> Unit
) {
    val l = profile.language
    val str = { fa: String, en: String -> if (l == "EN") en else fa }

    val coroutineScope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableStateOf(0f) }
    val view = LocalView.current
    
    val prizes = if (l == "EN") {
        listOf(
            Triple("COINS", 10, "10 Coins"),
            Triple("XP", 20, "20 XP"),
            Triple("COINS", 50, "50 Coins"),
            Triple("ITEM", 1, "Cosmic Item"),
            Triple("XP", 50, "50 XP"),
            Triple("COINS", 5, "5 Coins")
        )
    } else {
        listOf(
            Triple("COINS", 10, "۱۰ سکه"),
            Triple("XP", 20, "۲۰ XP"),
            Triple("COINS", 50, "۵۰ سکه"),
            Triple("ITEM", 1, "آیتم کیهانی"),
            Triple("XP", 50, "۵۰ XP"),
            Triple("COINS", 5, "۵ سکه")
        )
    }
    val sliceAngle = 360f / prizes.size

    val animatedRotation by animateFloatAsState(
        targetValue = rotationAngle,
        animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
        label = "wheel_spin"
    )

    var lastWheelTick by remember { mutableStateOf(-1) }
    LaunchedEffect(animatedRotation) {
        if (isSpinning) {
            val currentAngle = animatedRotation % 360
            val currentIndex = (((360f - currentAngle + sliceAngle / 2) % 360) / sliceAngle).toInt()
            if (currentIndex != lastWheelTick) {
                if (lastWheelTick != -1) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    SoundManager.playSound(SoundType.SPIN_WHEEL_TICK)
                }
                lastWheelTick = currentIndex
            }
        } else {
            lastWheelTick = -1
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(str("گردونه شانس ✨", "Wheel of Fortune ✨"), fontWeight = FontWeight.Bold, color = activeTheme.primaryColor, fontSize = 24.sp)
        Text(str("هر چرخش ۲۰ سکه هزینه دارد.", "Each spin costs 20 coins."), color = activeTheme.secondaryFontColor, fontSize = 14.sp)

        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            // Shadow / Outer Gold Rim
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = size.minDimension / 2f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFBC02D), // inner edge
                    radius = size.minDimension / 2f - 4.dp.toPx(),
                    center = center,
                    style = Stroke(width = 8.dp.toPx())
                )
            }

            // Wheel pieces
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .graphicsLayer { rotationZ = animatedRotation }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val colors = listOf(
                        Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFFB300),
                        Color(0xFF43A047), Color(0xFF00ACC1), Color(0xFF1E88E5),
                        Color(0xFF5E35B1), Color(0xFFD81B60)
                    )
                    var startAngle = -90f
                    for (i in prizes.indices) {
                        drawArc(
                            color = colors[i % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sliceAngle,
                            useCenter = true,
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height)
                        )
                        startAngle += sliceAngle
                    }
                    
                    // Separator lines
                    startAngle = -90f
                    for (i in prizes.indices) {
                        val rad = Math.toRadians(startAngle.toDouble())
                        drawLine(
                            color = Color(0x66000000), // semi transparent black line
                            start = center,
                            end = Offset(
                                x = center.x + (cos(rad) * size.minDimension / 2).toFloat(),
                                y = center.y + (sin(rad) * size.minDimension / 2).toFloat()
                            ),
                            strokeWidth = 2.dp.toPx()
                        )
                        startAngle += sliceAngle
                    }
                }

                prizes.forEachIndexed { index, prize ->
                    // Simplified wheel visual: just text placed in a circle offset
                    val angleOffset = index * sliceAngle + (sliceAngle / 2)
                    val r = 85.dp.value
                    val angleRad = Math.toRadians((angleOffset - 90).toDouble())
                    val tx = (r * cos(angleRad)).toFloat()
                    val ty = (r * sin(angleRad)).toFloat()
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = tx.dp, y = ty.dp)
                            .graphicsLayer { rotationZ = angleOffset + 90 }
                    ) {
                        Text(prize.third, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            // Center Knob
            Canvas(modifier = Modifier.size(56.dp)) {
                drawCircle(color = Color(0xFFFFD700), radius = size.minDimension / 2f)
                drawCircle(color = Color(0xFF1E88E5), radius = size.minDimension / 2f - 4.dp.toPx())
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f), // highlight
                    radius = size.minDimension / 4f,
                    center = Offset(center.x - 4.dp.toPx(), center.y - 4.dp.toPx())
                )
            }

            // Pointer (Triangle at the top)
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp)
                    .size(width = 30.dp, height = 36.dp)
            ) {
                val path = Path().apply {
                    moveTo(size.width * 0.1f, 0f)
                    lineTo(size.width * 0.9f, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                // Outline/shadow
                drawPath(path = path, color = Color(0xFF666666), style = Stroke(width = 6.dp.toPx()))
                // Foreground Fill
                drawPath(path = path, color = Color.White)
            }
        }

        Button(
            onClick = {
                if (profile.coins >= 20 && !isSpinning) {
                    isSpinning = true
                    coroutineScope.launch {
                        val spins = 3 + (0..3).random()
                        val randomPrizeIndex = prizes.indices.random()
                        val targetAngle = (spins * 360f) + (360f - (randomPrizeIndex * sliceAngle))
                        rotationAngle += targetAngle
                        delay(3100) // wait for animation
                        onSpinComplete(prizes[randomPrizeIndex].first, prizes[randomPrizeIndex].second)
                        isSpinning = false
                        SoundManager.playSound(SoundType.COIN)
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }
                }
            },
            enabled = profile.coins >= 20 && !isSpinning,
            colors = ButtonDefaults.buttonColors(containerColor = activeTheme.primaryColor)
        ) {
            Text(if (isSpinning) str("در حال چرخش...", "Spinning...") else str("چرخش (۲۰ سکه)", "Spin (20 Coins)"), fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(str("کلکسیون کیهانی شما", "Your Cosmic Collection"), fontWeight = FontWeight.Bold, color = activeTheme.primaryColor, fontSize = 18.sp)
        inventoryItems.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().glassmorphism(cornerRadius = 12.dp, surfaceColor = activeTheme.surfaceColor, fillAlpha = 0.4f).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.emoji, fontSize = 24.sp)
                    Text(item.name, color = activeTheme.fontColor)
                }
                Text(str("تعداد: ${item.quantity}", "Count: ${item.quantity}"), color = activeTheme.secondaryFontColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ParticleBurst(modifier: Modifier = Modifier) {
    val particles = remember { List(15) { 
        Pair((0..360).random().toFloat(), (30..100).random().toFloat()) 
    } }
    
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val emojis = listOf("✨", "🌟", "🪙", "⭐")
        particles.forEachIndexed { index, (angle, dist) ->
            val angleRad = Math.toRadians(angle.toDouble())
            val tx = (dist * animationProgress.value * cos(angleRad)).toFloat()
            val ty = (dist * animationProgress.value * sin(angleRad)).toFloat()
            val alpha = (1f - animationProgress.value).coerceIn(0f, 1f)
            
            Text(
                text = emojis[index % emojis.size],
                fontSize = 16.sp,
                modifier = Modifier
                    .offset(x = tx.dp, y = ty.dp)
                    .graphicsLayer { this.alpha = alpha }
            )
        }
    }
}
