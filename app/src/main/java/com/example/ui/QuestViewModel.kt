package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll
import java.util.Calendar

class QuestViewModel(application: Application) : AndroidViewModel(application) {

    var showDailyRewardDialog by mutableStateOf(false)
        private set

    private fun updateWidget() {
        viewModelScope.launch {
            try {
                com.example.ui.widget.QuestWidget().updateAll(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val repository: QuestRepository

    val quests: StateFlow<List<Quest>>
    val userProfile: StateFlow<UserProfile?>
    val themes: StateFlow<List<ThemeUnlock>>
    val inventoryItems: StateFlow<List<InventoryItem>>
    val kingdomBuildings: StateFlow<List<KingdomBuilding>>

    init {
        val database = QuestDatabase.getDatabase(application)
        repository = QuestRepository(database.questDao())

        // Fetch data reactively
        quests = repository.allQuests.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        userProfile = repository.userProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        themes = repository.themeUnlocks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        inventoryItems = repository.inventoryItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        kingdomBuildings = repository.kingdomBuildings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Prepopulate db if empty
        viewModelScope.launch {
            repository.initializeIfNeeded()
            checkDailyReward()
        }
    }

    private fun checkDailyReward() {
        val prefs = getApplication<Application>().getSharedPreferences("quest_prefs", Context.MODE_PRIVATE)
        val lastClaimDate = prefs.getLong("last_daily_reward", 0L)
        val currentTime = System.currentTimeMillis()
        
        val lastCalendar = Calendar.getInstance().apply { timeInMillis = lastClaimDate }
        val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentTime }
        
        val isSameDay = lastCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                        lastCalendar.get(Calendar.DAY_OF_YEAR) == currentCalendar.get(Calendar.DAY_OF_YEAR)
                        
        if (!isSameDay) {
            showDailyRewardDialog = true
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val profile = userProfile.firstOrNull() ?: return@launch
            // We use raw insert/update but QuestRepository doesn't expose update profile directly except through insertUserProfile which might use @Insert(onConflict = REPLACE)
            // Looking at QuestRepository: insertUserProfile handles it.
            repository.insertUserProfile(profile.copy(coins = profile.coins + 50))
            
            val prefs = getApplication<Application>().getSharedPreferences("quest_prefs", Context.MODE_PRIVATE)
            prefs.edit().putLong("last_daily_reward", System.currentTimeMillis()).apply()
            
            showDailyRewardDialog = false
        }
    }

    fun dismissDailyRewardDialog() {
        showDailyRewardDialog = false
    }

    fun addQuest(title: String, description: String, questType: String, category: String, difficulty: String, isBoss: Boolean, reminderTime: Long? = null, reminderIntervalMinutes: Int? = null) {
        viewModelScope.launch {
            // Determine rewards based on difficulty
            val (xp, coins) = when (difficulty) {
                "EASY" -> 15 to 5
                "MEDIUM" -> 30 to 12
                "HARD" -> 60 to 25
                else -> 15 to 5
            }
            
            // if boss, maybe high HP. For simplicity let's make maxBossHp related to difficulty.
            val bossHp = if (isBoss) {
                when (difficulty) {
                    "EASY" -> 100
                    "MEDIUM" -> 300
                    "HARD" -> 500
                    else -> 100
                }
            } else 0

            val newQuest = Quest(
                title = title,
                description = description,
                questType = questType,
                category = category,
                difficulty = difficulty,
                xpReward = xp,
                coinsReward = coins,
                isCompleted = false,
                isBoss = isBoss,
                maxBossHp = bossHp,
                currentBossHp = bossHp,
                reminderTime = reminderTime,
                reminderIntervalMinutes = reminderIntervalMinutes
            )
            val questId = repository.insertQuest(newQuest).toInt() // Assume insertQuest returns rowId OR just insert returning ID? wait. Room insert annotations return Long.
            if (reminderTime != null) {
                scheduleReminder(questId, title, description, reminderTime, reminderIntervalMinutes)
            }
            updateWidget()
        }
    }

    private fun scheduleReminder(questId: Int, title: String, description: String, reminderTime: Long, reminderIntervalMinutes: Int?) {
        val am = getApplication<Application>().getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(getApplication(), com.example.ui.ReminderReceiver::class.java).apply {
            putExtra("QUEST_TITLE", title)
            putExtra("QUEST_DESC", description)
            putExtra("QUEST_ID", questId)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            getApplication(),
            questId,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // For Android 12+, we need Exact Alarm permission. For simpicity, we can use setExactAndAllowWhileIdle if we check permission, but set is safer if not exact.
        // Or if repeating, setRepeating.
        
        try {
            if (reminderIntervalMinutes != null && reminderIntervalMinutes > 0) {
                val intervalMilli = reminderIntervalMinutes * 60 * 1000L
                am.setRepeating(android.app.AlarmManager.RTC_WAKEUP, reminderTime, intervalMilli, pendingIntent)
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                    } else {
                         am.set(android.app.AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                    }
                } else {
                    am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectCharacterClass(charClass: String) {
        viewModelScope.launch {
            val profile = repository.userProfile.firstOrNull() ?: return@launch
            repository.insertUserProfile(profile.copy(characterClass = charClass))
        }
    }

    fun attackBoss(quest: Quest) {
        viewModelScope.launch {
            if (quest.isCompleted || !quest.isBoss) return@launch
            val damage = 50 // Fixed damage per sub-task completion simulation
            val newHp = (quest.currentBossHp - damage).coerceAtLeast(0)
            
            if (newHp == 0) {
                // Boss defeated!
                val updatedQuest = quest.copy(currentBossHp = 0)
                repository.updateQuest(updatedQuest)
                toggleQuestCompletion(updatedQuest) // Mark as completed and reward
            } else {
                repository.updateQuest(quest.copy(currentBossHp = newHp))
            }
        }
    }

    fun toggleQuestCompletion(quest: Quest) {
        viewModelScope.launch {
            val newIsCompleted = !quest.isCompleted
            val updatedQuest = quest.copy(
                isCompleted = newIsCompleted,
                completedAt = if (newIsCompleted) System.currentTimeMillis() else null
            )
            repository.updateQuest(updatedQuest)

            // Adjust profile XP and Coins based on transition
            val profile = repository.userProfile.firstOrNull() ?: return@launch
            var xpChange = if (newIsCompleted) quest.xpReward else -quest.xpReward
            var coinsChange = if (newIsCompleted) quest.coinsReward else -quest.coinsReward

            // Handle class bonus
            if (newIsCompleted) {
                val classBonus = when (profile.characterClass) {
                    "WARRIOR" -> if (quest.category == "PHYSICAL") (xpChange * 0.2).toInt() else 0
                    "MAGE" -> if (quest.category == "MENTAL") (xpChange * 0.2).toInt() else 0
                    "ROGUE" -> if (quest.category == "CREATIVE" || quest.category == "DAILY") (xpChange * 0.2).toInt() else 0
                    else -> 0
                }
                xpChange += classBonus
            }

            // Streak & Combo Logic
            var newStreak = profile.streakDays
            var newCompletedToday = profile.questsCompletedToday
            val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val lastQuestStr = if (profile.lastQuestDate > 0) java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(profile.lastQuestDate)) else ""
            
            if (newIsCompleted) {
                if (lastQuestStr != todayDateStr) {
                    val yesterdayCalendar = java.util.Calendar.getInstance()
                    yesterdayCalendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    val yesterdayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(yesterdayCalendar.time)
                    
                    if (lastQuestStr == yesterdayStr) {
                        newStreak += 1
                    } else {
                        newStreak = 1
                    }
                    newCompletedToday = 1
                } else {
                    newCompletedToday += 1
                }

                if (newCompletedToday >= 3) {
                    xpChange = (xpChange * 1.5).toInt()
                    coinsChange = (coinsChange * 1.5).toInt()
                }
            } else {
                if (lastQuestStr == todayDateStr) {
                    newCompletedToday = (newCompletedToday - 1).coerceAtLeast(0)
                    if (newCompletedToday == 0) {
                        newStreak = (newStreak - 1).coerceAtLeast(0)
                    }
                }
            }

            // Adjust profile stats based on category
            var str = profile.strength
            var intel = profile.intelligence
            var cha = profile.charisma
            var dis = profile.discipline

            val statChange = if (newIsCompleted) 1 else -1
            when (quest.category) {
                "PHYSICAL" -> str += statChange
                "MENTAL" -> intel += statChange
                "CREATIVE" -> cha += statChange
                "DAILY" -> dis += statChange
            }

            // Adjust pet XP
            var newPetXp = profile.petXp + statChange
            var newPetLevel = profile.petLevel
            if (newPetXp < 0) newPetXp = 0
            if (newPetXp >= newPetLevel * 10) {
                newPetXp = 0
                newPetLevel += 1
            } else if (newPetLevel > 1 && profile.petXp == 0 && statChange < 0) {
                 // handle pet scaling down if massive quests withdrawn
            }

            // Combine stats with calculated levels
            val baseProfile = calculateXpAndCoins(profile, xpChange, coinsChange)
            
            val updatedProfile = baseProfile.copy(
                strength = str.coerceAtLeast(0),
                intelligence = intel.coerceAtLeast(0),
                charisma = cha.coerceAtLeast(0),
                discipline = dis.coerceAtLeast(0),
                petXp = newPetXp,
                petLevel = newPetLevel,
                lastActiveDate = System.currentTimeMillis(), // Update last active date!
                streakDays = newStreak,
                lastQuestDate = if (newIsCompleted) System.currentTimeMillis() else profile.lastQuestDate,
                questsCompletedToday = newCompletedToday
            )

            repository.insertUserProfile(updatedProfile)
            updateWidget()
        }
    }

    fun deleteQuest(id: Int) {
        viewModelScope.launch {
            repository.deleteQuestById(id)
            updateWidget()
        }
    }

    fun applyLootReward(prizeType: String, amount: Int) {
        viewModelScope.launch {
            val profile = repository.userProfile.firstOrNull() ?: return@launch
            var newXp = profile.xp
            var newCoins = profile.coins - 20 // Deduct cost
            
            if (prizeType == "COINS") {
                newCoins += amount
            } else if (prizeType == "XP") {
                newXp += amount
            } else if (prizeType == "ITEM") {
                // Add cosmic item
                val existing = repository.inventoryItems.firstOrNull()?.find { it.id == "COSMIC_DUST" }
                val newItem = existing?.copy(quantity = existing.quantity + 1) ?: InventoryItem("COSMIC_DUST", "غبار کیهانی", "یک آیتم بسیار نادر!", "✨", 1)
                repository.insertInventoryItem(newItem)
            }
            
            repository.insertUserProfile(calculateXpAndCoins(profile.copy(coins = newCoins.coerceAtLeast(0), xp = newXp), 0, 0))
        }
    }

    fun buildKingdomBuilding(buildingId: String) {
        viewModelScope.launch {
            val profile = repository.userProfile.firstOrNull() ?: return@launch
            val buildings = repository.kingdomBuildings.firstOrNull() ?: return@launch
            val building = buildings.find { it.id == buildingId } ?: return@launch
            
            if (!building.isBuilt && profile.coins >= building.cost) {
                repository.insertUserProfile(profile.copy(coins = profile.coins - building.cost))
                repository.insertKingdomBuilding(building.copy(isBuilt = true))
            }
        }
    }
    
    fun toggleDarkMode() {
        viewModelScope.launch {
            val profile = repository.userProfile.firstOrNull() ?: return@launch
            repository.insertUserProfile(profile.copy(isDarkMode = !profile.isDarkMode))
        }
    }
    
    fun toggleLanguage() {
        viewModelScope.launch {
            val profile = repository.userProfile.firstOrNull() ?: return@launch
            repository.insertUserProfile(profile.copy(language = if (profile.language == "FA") "EN" else "FA"))
        }
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            val profile = repository.userProfile.firstOrNull() ?: return@launch
            repository.insertUserProfile(profile.copy(hasSeenOnboarding = true))
        }
    }

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            val profile = repository.userProfile.firstOrNull() ?: return@launch
            // Verify if the theme is unlocked before selecting
            val unlockedThemes = repository.themeUnlocks.firstOrNull() ?: emptyList()
            val isFreeTheme = com.example.ui.theme.ThemePresets.getAllThemes().find { it.themeId == themeId }?.isFree == true
            if (isFreeTheme || unlockedThemes.any { it.themeId == themeId && it.isUnlocked }) {
                repository.insertUserProfile(profile.copy(currentTheme = themeId))
            }
        }
    }

    fun unlockTheme(themeId: String, cost: Int) {
        viewModelScope.launch {
            val profile = repository.userProfile.firstOrNull() ?: return@launch
            if (profile.coins >= cost) {
                // Deduct coins
                val updatedProfile = profile.copy(coins = profile.coins - cost, currentTheme = themeId)
                repository.insertUserProfile(updatedProfile)

                // Set theme status to unlocked
                repository.insertThemeUnlock(ThemeUnlock(themeId = themeId, isUnlocked = true))
            }
        }
    }

    // Helper logic to handle dynamic progression / leveling up
    private fun calculateXpAndCoins(profile: UserProfile, xpChange: Int, coinsChange: Int): UserProfile {
        var currentXp = profile.xp + xpChange
        var currentLevel = profile.level
        var currentCoins = profile.coins + coinsChange

        // Prevent negative coins & XP
        if (currentCoins < 0) currentCoins = 0
        if (currentXp < 0) {
            // If they cancel a quest, they can go back to previous level if XP is negative,
            // but for a robust game, let's keep it safe. Just bound XP at 0.
            currentXp = 0
        }

        // Check for positive level up: Each level demands (level * 100) XP
        var xpRequired = currentLevel * 100
        while (currentXp >= xpRequired) {
            currentXp -= xpRequired
            currentLevel++
            // Level Up Bonus!
            currentCoins += currentLevel * 10
            xpRequired = currentLevel * 100
        }

        // Check for reverse level down (if user unchecks a massive quest and xp gets pulled):
        // Highly edge-case, but handled to prevent overflow or strange levels.
        while (currentLevel > 1 && currentXp < 0) {
            currentLevel--
            xpRequired = currentLevel * 100
            currentXp += xpRequired
        }

        return profile.copy(
            level = currentLevel,
            xp = currentXp,
            coins = currentCoins
        )
    }
}
