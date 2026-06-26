package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    // Quests
    @Query("SELECT * FROM quests ORDER BY createdAt DESC")
    fun getAllQuests(): Flow<List<Quest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: Quest): Long

    @Update
    suspend fun updateQuest(quest: Quest)

    @Query("DELETE FROM quests WHERE id = :id")
    suspend fun deleteQuestById(id: Int)

    // User Profile (Only one row with ID = 1)
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfile)

    // Theme Unlocks
    @Query("SELECT * FROM theme_unlocks")
    fun getAllThemeUnlocks(): Flow<List<ThemeUnlock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThemeUnlock(themeUnlock: ThemeUnlock)

    // Inventory
    @Query("SELECT * FROM inventory_items")
    fun getAllInventoryItems(): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem)

    // Kingdom
    @Query("SELECT * FROM kingdom_buildings")
    fun getAllKingdomBuildings(): Flow<List<KingdomBuilding>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKingdomBuilding(building: KingdomBuilding)
}
