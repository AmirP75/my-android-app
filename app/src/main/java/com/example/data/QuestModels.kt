package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class SubTask(val id: Int, val title: String, var isCompleted: Boolean = false)

class SubTaskConverter {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(List::class.java, SubTask::class.java)
    private val adapter = moshi.adapter<List<SubTask>>(type)

    @TypeConverter
    fun fromSubTasks(subTasks: List<SubTask>?): String {
        return adapter.toJson(subTasks ?: emptyList())
    }

    @TypeConverter
    fun toSubTasks(subTasksString: String?): List<SubTask> {
        return if (subTasksString.isNullOrEmpty()) emptyList() else adapter.fromJson(subTasksString) ?: emptyList()
    }
}

@Entity(tableName = "quests")
@TypeConverters(SubTaskConverter::class)
data class Quest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val questType: String, // "MAIN" (ماموریت اصلی) or "SIDE" (ماموریت فرعی)
    val category: String = "DAILY", // "PHYSICAL", "MENTAL", "CREATIVE", "DAILY"
    val difficulty: String, // "EASY" (آسان), "MEDIUM" (متوسط), "HARD" (سخت)
    val isCompleted: Boolean = false,
    val xpReward: Int = 10,
    val coinsReward: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isBoss: Boolean = false,
    val maxBossHp: Int = 0,
    val currentBossHp: Int = 0,
    val subTasks: List<SubTask> = emptyList(),
    val reminderTime: Long? = null,
    val reminderIntervalMinutes: Int? = null
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val xp: Int = 0,
    val coins: Int = 0,
    val currentTheme: String = "CLASSIC",
    val characterClass: String = "UNSELECTED", // "WARRIOR", "MAGE", "ROGUE", "UNSELECTED"
    val strength: Int = 0, // from PHYSICAL
    val intelligence: Int = 0, // from MENTAL
    val charisma: Int = 0, // from CREATIVE
    val discipline: Int = 0, // from DAILY / completion rate
    val lastActiveDate: Long = System.currentTimeMillis(),
    val petLevel: Int = 1,
    val petXp: Int = 0,
    val language: String = "FA", // "FA" or "EN"
    val isDarkMode: Boolean = true,
    val streakDays: Int = 0,
    val lastQuestDate: Long = 0L,
    val questsCompletedToday: Int = 0,
    val hasSeenOnboarding: Boolean = false
)

@Entity(tableName = "theme_unlocks")
data class ThemeUnlock(
    @PrimaryKey val themeId: String,
    val isUnlocked: Boolean = false
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val quantity: Int = 1
)

@Entity(tableName = "kingdom_buildings")
data class KingdomBuilding(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val cost: Int,
    val isBuilt: Boolean = false,
    val xPos: Float, // Simplified positioning
    val yPos: Float
)
