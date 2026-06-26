package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class QuestRepository(private val questDao: QuestDao) {

    val allQuests: Flow<List<Quest>> = questDao.getAllQuests()
    val userProfile: Flow<UserProfile?> = questDao.getUserProfile()
    val themeUnlocks: Flow<List<ThemeUnlock>> = questDao.getAllThemeUnlocks()
    val inventoryItems: Flow<List<InventoryItem>> = questDao.getAllInventoryItems()
    val kingdomBuildings: Flow<List<KingdomBuilding>> = questDao.getAllKingdomBuildings()

    suspend fun insertQuest(quest: Quest): Long {
        return questDao.insertQuest(quest)
    }

    suspend fun updateQuest(quest: Quest) {
        questDao.updateQuest(quest)
    }

    suspend fun deleteQuestById(id: Int) {
        questDao.deleteQuestById(id)
    }

    suspend fun insertUserProfile(userProfile: UserProfile) {
        questDao.insertUserProfile(userProfile)
    }

    suspend fun insertThemeUnlock(themeUnlock: ThemeUnlock) {
        questDao.insertThemeUnlock(themeUnlock)
    }

    suspend fun insertInventoryItem(item: InventoryItem) {
        questDao.insertInventoryItem(item)
    }

    suspend fun insertKingdomBuilding(building: KingdomBuilding) {
        questDao.insertKingdomBuilding(building)
    }

    /**
     * Initializes the database with default themes, profile, and starter quests if they do not exist.
     */
    suspend fun initializeIfNeeded() {
        val currentProfile = questDao.getUserProfile().firstOrNull()
        if (currentProfile == null) {
            // Setup default user profile
            questDao.insertUserProfile(
                UserProfile(
                    id = 1,
                    level = 1,
                    xp = 0,
                    coins = 15, // Starter gift
                    currentTheme = "CLASSIC"
                )
            )

            // Setup default Kingdom
            val initialBuildings = listOf(
                KingdomBuilding("CASTLE", "قلعه من", "🏰", 0, true, 0.5f, 0.3f),
                KingdomBuilding("MARKET", "بازارچه", "🏪", 50, false, 0.2f, 0.5f),
                KingdomBuilding("LIBRARY", "کتابخانه جادو", "📚", 100, false, 0.8f, 0.5f),
                KingdomBuilding("GARDEN", "باغ آرامش", "🌳", 150, false, 0.3f, 0.8f),
                KingdomBuilding("TOWER", "برج مراقبت", "🗼", 200, false, 0.7f, 0.8f)
            )
            initialBuildings.forEach { questDao.insertKingdomBuilding(it) }

            // Setup default theme unlocks
            val defaultThemes = listOf(
                ThemeUnlock("CLASSIC", isUnlocked = true),
                ThemeUnlock("GOLDEN_KINGDOM", isUnlocked = false),
                ThemeUnlock("NEON_SYNTH", isUnlocked = false),
                ThemeUnlock("FOREST_SANCTUARY", isUnlocked = false),
                ThemeUnlock("COSMIC_NEBULA", isUnlocked = false)
            )
            for (theme in defaultThemes) {
                questDao.insertThemeUnlock(theme)
            }

            // Setup starter quests
            val starterQuests = listOf(
                Quest(
                    title = "آموزش اولیه: آغاز قهرمانی",
                    description = "یک ماموریت جدید برای خودتان بسازید و آن را با موفقیت تمام کنید!",
                    questType = "MAIN",
                    difficulty = "EASY",
                    xpReward = 15,
                    coinsReward = 10
                ),
                Quest(
                    title = "ورزش دلاورانه",
                    description = "۲۰ دقیقه فعالیت فدرتمند بدنی برای آمادگی نبرد زندگی",
                    questType = "SIDE",
                    difficulty = "MEDIUM",
                    xpReward = 30,
                    coinsReward = 15
                ),
                Quest(
                    title = "مطالعه کتیبه‌های خِرد",
                    description = "مطالعه ۱۰ صفحه از کتیبه (کتاب) مورد نظر جهت افزایش علم و تمرکز",
                    questType = "SIDE",
                    difficulty = "EASY",
                    xpReward = 15,
                    coinsReward = 5
                )
            )
            for (quest in starterQuests) {
                questDao.insertQuest(quest)
            }
        }
    }
}
