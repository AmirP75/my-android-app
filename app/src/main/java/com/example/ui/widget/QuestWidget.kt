package com.example.ui.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.height
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.color.ColorProvider
import com.example.data.Quest
import com.example.data.QuestDatabase
import kotlinx.coroutines.flow.first

val questIdKey = ActionParameters.Key<Int>("questId")

class QuestWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuestWidget()
}

class QuestWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Fetch initially
        val dao = QuestDatabase.getDatabase(context).questDao()
        val quests = dao.getAllQuests().first().filter { !it.isCompleted }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(android.graphics.Color.parseColor("#120e24")), Color(android.graphics.Color.parseColor("#120e24"))))
                        .padding(16.dp)
                        .cornerRadius(16.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Quests",
                            style = TextStyle(
                                color = ColorProvider(Color(android.graphics.Color.WHITE), Color(android.graphics.Color.WHITE)),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Image(
                            provider = ImageProvider(android.R.drawable.ic_popup_sync),
                            contentDescription = "Refresh",
                            colorFilter = ColorFilter.tint(ColorProvider(Color(android.graphics.Color.parseColor("#E6C200")), Color(android.graphics.Color.parseColor("#E6C200")))),
                            modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>())
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    if (quests.isEmpty()) {
                        Text(
                            text = "No active quests!",
                            style = TextStyle(color = ColorProvider(Color(android.graphics.Color.LTGRAY), Color(android.graphics.Color.LTGRAY)), fontSize = 14.sp)
                        )
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(quests.take(5)) { quest ->
                                QuestItemWidget(quest)
                            }
                        }
                    }
                }
            }
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        QuestWidget().updateAll(context)
    }
}

class CompleteQuestAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val questId = parameters[questIdKey] ?: return
        val dao = QuestDatabase.getDatabase(context).questDao()
        val quests = dao.getAllQuests().first()
        val quest = quests.find { it.id == questId }
        if (quest != null) {
            dao.updateQuest(quest.copy(isCompleted = true))
            
            val profile = dao.getUserProfile().first()
            if (profile != null) {
                var xpGain = if (quest.questType == "MAIN") 50 else 20
                if (quest.difficulty == "HARD") xpGain += 30 else if (quest.difficulty == "MEDIUM") xpGain += 10
                
                var newXp = profile.xp + xpGain
                var newLevel = profile.level
                var newCoins = profile.coins + (xpGain / 2)
                
                val xpNeeded = newLevel * 100
                if (newXp >= xpNeeded) {
                    newLevel++
                    newXp -= xpNeeded
                }
                
                dao.insertUserProfile(profile.copy(xp = newXp, level = newLevel, coins = newCoins))
            }
        }
        QuestWidget().updateAll(context)
    }
}

@androidx.compose.runtime.Composable
fun QuestItemWidget(quest: Quest) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(ColorProvider(Color(android.graphics.Color.parseColor("#2b184b")), Color(android.graphics.Color.parseColor("#2b184b"))))
            .cornerRadius(8.dp)
            .padding(12.dp)
            .clickable(actionRunCallback<CompleteQuestAction>(actionParametersOf(questIdKey to quest.id))), // Tap completes the quest
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(android.R.drawable.checkbox_off_background),
            contentDescription = "Check",
            colorFilter = ColorFilter.tint(ColorProvider(Color(android.graphics.Color.parseColor("#E6C200")), Color(android.graphics.Color.parseColor("#E6C200")))),
            modifier = GlanceModifier.clickable(actionRunCallback<CompleteQuestAction>(actionParametersOf(questIdKey to quest.id)))
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = quest.title,
            maxLines = 1,
            style = TextStyle(color = ColorProvider(Color(android.graphics.Color.WHITE), Color(android.graphics.Color.WHITE)), fontSize = 14.sp),
            modifier = GlanceModifier.defaultWeight()
        )
    }
}
