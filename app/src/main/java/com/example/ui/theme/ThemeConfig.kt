package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

data class RpgThemeStyle(
    val themeId: String,
    val titlePersian: String,
    val description: String,
    val cost: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val shadowColor: Color,
    val fontColor: Color = Color.White,
    val secondaryFontColor: Color = Color(0xFFD1D5DB),
    val iconEmoji: String,
    val gradientColors: List<Color>
) {
    val isFree: Boolean get() = cost == 0
}

@Composable
fun animateRpgThemeAsState(targetTheme: RpgThemeStyle): RpgThemeStyle {
    val duration = 500
    val pColor by animateColorAsState(targetTheme.primaryColor, tween(duration), label = "pColor")
    val sColor by animateColorAsState(targetTheme.secondaryColor, tween(duration), label = "sColor")
    val bgColor by animateColorAsState(targetTheme.backgroundColor, tween(duration), label = "bgColor")
    val surColor by animateColorAsState(targetTheme.surfaceColor, tween(duration), label = "surColor")
    val shColor by animateColorAsState(targetTheme.shadowColor, tween(duration), label = "shColor")
    val fColor by animateColorAsState(targetTheme.fontColor, tween(duration), label = "fColor")
    val sfColor by animateColorAsState(targetTheme.secondaryFontColor, tween(duration), label = "sfColor")
    val g0 by animateColorAsState(targetTheme.gradientColors.getOrElse(0) { Color.Transparent }, tween(duration), label = "g0")
    val g1 by animateColorAsState(targetTheme.gradientColors.getOrElse(1) { Color.Transparent }, tween(duration), label = "g1")

    return targetTheme.copy(
        primaryColor = pColor,
        secondaryColor = sColor,
        backgroundColor = bgColor,
        surfaceColor = surColor,
        shadowColor = shColor,
        fontColor = fColor,
        secondaryFontColor = sfColor,
        gradientColors = listOf(g0, g1)
    )
}

object ThemePresets {
    val CLASSIC = RpgThemeStyle(
        themeId = "CLASSIC",
        titlePersian = "آر‌پي‌جی سنتی 🧙‍♂️",
        description = "احساس نوستالژیک ماجراجویی با رنگ‌های زیبای جادویی.",
        cost = 0,
        primaryColor = Color(0xFF6750A4), // Deep M3 purple
        secondaryColor = Color(0xFFD0BCFF), // M3 lavender
        backgroundColor = Color(0xFFFDF7FF), // Bright soft lavender-grey
        surfaceColor = Color(0xFFFFFFFF), // White paper card
        shadowColor = Color(0xFF6750A4),
        fontColor = Color(0xFF21005D), // Dark purple text
        secondaryFontColor = Color(0xFF49454F), // Medium purple grey
        iconEmoji = "🧙‍♂️",
        gradientColors = listOf(Color(0xFFEADDFF), Color(0xFFFDF7FF))
    )

    val GOLDEN_KINGDOM = RpgThemeStyle(
        themeId = "GOLDEN_KINGDOM",
        titlePersian = "پادشاهی طلایی 👑",
        description = "عظمت دربار سلطنتی با جلال طلا و لاجوردی خیره‌کننده.",
        cost = 50,
        primaryColor = Color(0xFFB58900), // Royal gold leaf
        secondaryColor = Color(0xFFFFDF7A), // Royal pale yellow
        backgroundColor = Color(0xFFFFFDF5), // Bright cozy warm ivory
        surfaceColor = Color(0xFFFFFFFF),
        shadowColor = Color(0xFFB58900),
        fontColor = Color(0xFF5C3C00), // Rich brown
        secondaryFontColor = Color(0xFF7A643F),
        iconEmoji = "👑",
        gradientColors = listOf(Color(0xFFFFECB3), Color(0xFFFFFDF5))
    )

    val NEON_SYNTH = RpgThemeStyle(
        themeId = "NEON_SYNTH",
        titlePersian = "نئون سایبرپانک ⚡",
        description = "سرعت و نبردهای فیزیکی در خیابان‌های نورانی فردا.",
        cost = 100,
        primaryColor = Color(0xFFFF007F), // Neon cyber pink
        secondaryColor = Color(0xFF00E5FF), // Cyber cyan
        backgroundColor = Color(0xFFFFF0F5), // Neon soft touch pink
        surfaceColor = Color(0xFFFFFFFF),
        shadowColor = Color(0xFFFF007F),
        fontColor = Color(0xFF4A0033),
        secondaryFontColor = Color(0xFF7A4B6D),
        iconEmoji = "⚡",
        gradientColors = listOf(Color(0xFFFFD1E8), Color(0xFFFFF0F5))
    )

    val FOREST_SANCTUARY = RpgThemeStyle(
        themeId = "FOREST_SANCTUARY",
        titlePersian = "پناهگاه جنگلی 🍃",
        description = "سکوت، آرامش روح و نیروهای الف‌ها و کاهنان طبیعت.",
        cost = 150,
        primaryColor = Color(0xFF2E7D32), // Deep forest green
        secondaryColor = Color(0xFFA5D6A7),
        backgroundColor = Color(0xFFF1F8E9), // Forest light glade
        surfaceColor = Color(0xFFFFFFFF),
        shadowColor = Color(0xFF2E7D32),
        fontColor = Color(0xFF1B5E20),
        secondaryFontColor = Color(0xFF4E7052),
        iconEmoji = "🍃",
        gradientColors = listOf(Color(0xFFDCEDC8), Color(0xFFF1F8E9))
    )

    val COSMIC_NEBULA = RpgThemeStyle(
        themeId = "COSMIC_NEBULA",
        titlePersian = "سحابی کیهانی 🌌",
        description = "انرژی‌های بی‌انتهای کهکشانی و جادوهای اختری دوردست.",
        cost = 200,
        primaryColor = Color(0xFF006064), // Cosmic cyan
        secondaryColor = Color(0xFFE0F7FA),
        backgroundColor = Color(0xFFE0F2F1), // Space vapor glint
        surfaceColor = Color(0xFFFFFFFF),
        shadowColor = Color(0xFF006064),
        fontColor = Color(0xFF004D40),
        secondaryFontColor = Color(0xFF3B6E67),
        iconEmoji = "🌌",
        gradientColors = listOf(Color(0xFFB2DFDB), Color(0xFFE0F2F1))
    )

    val OCEAN_DEPTHS = RpgThemeStyle(
        themeId = "OCEAN_DEPTHS",
        titlePersian = "اعماق اقیانوس 🌊",
        description = "قدرت امواج خروشان و اسرار نهفته در ژرفای دریا.",
        cost = 250,
        primaryColor = Color(0xFF0277BD),
        secondaryColor = Color(0xFFB3E5FC),
        backgroundColor = Color(0xFFE1F5FE),
        surfaceColor = Color(0xFFFFFFFF),
        shadowColor = Color(0xFF0277BD),
        fontColor = Color(0xFF01579B),
        secondaryFontColor = Color(0xFF4286B5),
        iconEmoji = "🌊",
        gradientColors = listOf(Color(0xFF81D4FA), Color(0xFFE1F5FE))
    )

    val VOLCANIC_FORGE = RpgThemeStyle(
        themeId = "VOLCANIC_FORGE",
        titlePersian = "کوره آتشفشانی 🌋",
        description = "حرارت و شور نبردهای حماسی با شعله‌های نابودگر.",
        cost = 300,
        primaryColor = Color(0xFFD84315),
        secondaryColor = Color(0xFFFFCCBC),
        backgroundColor = Color(0xFFFBE9E7),
        surfaceColor = Color(0xFFFFFFFF),
        shadowColor = Color(0xFFD84315),
        fontColor = Color(0xFFBF360C),
        secondaryFontColor = Color(0xFFD6694C),
        iconEmoji = "🌋",
        gradientColors = listOf(Color(0xFFFFAB91), Color(0xFFFBE9E7))
    )

    val SHADOW_REALM = RpgThemeStyle(
        themeId = "SHADOW_REALM",
        titlePersian = "قلمرو سایه‌ها 🌑",
        description = "تاریکی مطلق، اسرار مخفی و جادوهای پنهان در شب.",
        cost = 350,
        primaryColor = Color(0xFF424242),
        secondaryColor = Color(0xFFBDBDBD),
        backgroundColor = Color(0xFFF5F5F5),
        surfaceColor = Color(0xFFFFFFFF),
        shadowColor = Color(0xFF424242),
        fontColor = Color(0xFF212121),
        secondaryFontColor = Color(0xFF757575),
        iconEmoji = "🌑",
        gradientColors = listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5))
    )

    private val ALL_THEMES = listOf(
        CLASSIC,
        GOLDEN_KINGDOM,
        NEON_SYNTH,
        FOREST_SANCTUARY,
        COSMIC_NEBULA,
        OCEAN_DEPTHS,
        VOLCANIC_FORGE,
        SHADOW_REALM
    )

    fun getAllThemes(isDarkMode: Boolean = false): List<RpgThemeStyle> {
        return ALL_THEMES.map { getThemeById(it.themeId, isDarkMode) }
    }

    fun getThemeById(id: String, isDarkMode: Boolean = false): RpgThemeStyle {
        val baseTheme = ALL_THEMES.find { it.themeId == id } ?: CLASSIC
        if (!isDarkMode) return baseTheme

        // Convert to Dark Mode (tinted with primary color)
        val pc = baseTheme.primaryColor
        return baseTheme.copy(
            backgroundColor = Color(
                red = (pc.red * 0.1f + 0.05f).coerceIn(0f, 1f),
                green = (pc.green * 0.1f + 0.05f).coerceIn(0f, 1f),
                blue = (pc.blue * 0.1f + 0.05f).coerceIn(0f, 1f)
            ),
            surfaceColor = Color(
                red = (pc.red * 0.15f + 0.1f).coerceIn(0f, 1f),
                green = (pc.green * 0.15f + 0.1f).coerceIn(0f, 1f),
                blue = (pc.blue * 0.15f + 0.1f).coerceIn(0f, 1f)
            ),
            fontColor = Color.White,
            secondaryFontColor = Color(0xFFB0B0B0),
            gradientColors = listOf(baseTheme.primaryColor.copy(alpha = 0.4f), Color(0xFF101010))
        )
    }
}
