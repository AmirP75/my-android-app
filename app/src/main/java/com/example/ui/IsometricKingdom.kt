package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KingdomBuilding
import com.example.data.UserProfile
import com.example.ui.theme.RpgThemeStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

// Isometric math helpers
fun isoToScreen(x: Float, y: Float, z: Float = 0f, tileWidth: Float, tileHeight: Float): Offset {
    val screenX = (x - y) * (tileWidth / 2f)
    val screenY = (x + y) * (tileHeight / 2f) - z
    return Offset(screenX, screenY)
}

@Composable
fun IsometricScene(
    profile: UserProfile,
    kingdomBuildings: List<KingdomBuilding>,
    activeTheme: RpgThemeStyle
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val infiniteTransition = rememberInfiniteTransition()
    
    // Idle animation for characters / trees
    val idleAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Clouds movement
    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Bird movement
    val birdOffset by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val birdY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    
    val coroutineScope = rememberCoroutineScope()
    var charReaction by remember { mutableStateOf(false) }
    var selectedBuilding by remember { mutableStateOf<KingdomBuilding?>(null) }

    // Colors based on streak (Day/Night)
    val isDaytime = profile.streakDays > 0
    val skyColor = if (isDaytime) Color(0xFF64B5F6) else Color(0xFF283593)
    val groundColorTop = if (isDaytime) Color(0xFF81C784) else Color(0xFF455A64)
    val groundColorLeft = if (isDaytime) Color(0xFF4CAF50) else Color(0xFF37474F)
    val groundColorRight = if (isDaytime) Color(0xFF388E3C) else Color(0xFF263238)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(skyColor)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                    val maxOffset = 1000f * scale
                    offset = Offset(
                        x = (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                        y = (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        // simple hit detection
                        val centerX = size.width / 2f + offset.x
                        val centerY = size.height / 2f + offset.y
                        
                        // Check if character tapped (center)
                        val charDist = sqrt((tapOffset.x - centerX) * (tapOffset.x - centerX) + (tapOffset.y - centerY) * (tapOffset.y - centerY))
                        if(charDist < 100f * scale) {
                            if (!charReaction) {
                                charReaction = true
                                coroutineScope.launch {
                                    delay(500)
                                    charReaction = false
                                }
                            }
                            selectedBuilding = null
                            return@detectTapGestures
                        }
                        
                        // Check buildings (approximate based on grid positions)
                        val tileW = 100f * scale
                        val tileH = 50f * scale
                        var hit: KingdomBuilding? = null
                        for (building in kingdomBuildings.filter { it.isBuilt }) {
                            val px = building.xPos * 3f // spacing
                            val py = building.yPos * 3f
                            val screenPos = isoToScreen(px, py, 0f, tileW, tileH)
                            val bx = centerX + screenPos.x
                            val by = centerY + screenPos.y
                            val dist = sqrt((tapOffset.x - bx)*(tapOffset.x - bx) + (tapOffset.y - by)*(tapOffset.y - by))
                            if (dist < 80f * scale) {
                                hit = building
                                break
                            }
                        }
                        selectedBuilding = hit
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f + offset.x
            val cy = size.height / 2f + offset.y
            
            val tileW = 100f * scale
            val tileH = 50f * scale

            // Draw Clouds in background
            translate(left = cloudOffset * scale, top = size.height * 0.1f) {
                drawCloud(scale, if (isDaytime) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha=0.3f))
            }
            translate(left = (cloudOffset * 0.7f - 300f) * scale, top = size.height * 0.3f) {
                drawCloud(scale * 0.8f, if (isDaytime) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha=0.2f))
            }

            translate(left = cx, top = cy) {
                // 1. Draw Grid Ground
                val gridSize = 4
                for (x in -gridSize..gridSize) {
                    for (y in -gridSize..gridSize) {
                        drawIsometricTile(x.toFloat(), y.toFloat(), tileW, tileH, groundColorTop, groundColorLeft, groundColorRight)
                        // Environmental Trees on empty boundary coords
                        if ((x==-3 && y==-3) || (x==4 && y==-2) || (x==-4 && y==3)) {
                            val treePos = isoToScreen(x.toFloat(), y.toFloat(), 0f, tileW, tileH)
                            translate(left = treePos.x, top = treePos.y) {
                                drawTree(scale, idleAnimation, isDaytime)
                            }
                        }
                    }
                }
                
                // 2. Draw Built Buildings
                val sortedBuildings = kingdomBuildings.filter { it.isBuilt }.sortedBy { it.xPos + it.yPos }
                sortedBuildings.forEach { b ->
                    val bX = b.xPos * 3f
                    val bY = b.yPos * 3f
                    val sPos = isoToScreen(bX, bY, 0f, tileW, tileH)
                    translate(left = sPos.x, top = sPos.y) {
                        drawBuilding(scale, b.id, isDaytime)
                    }
                }

                // 3. Draw Character (Center)
                val charPush = if (charReaction) -30f * scale else 0f
                translate(left = 0f, top = charPush) {
                    drawHero(profile, scale, idleAnimation, isDaytime)
                }
                
                // 4. Draw Bird high up
                translate(left = birdOffset * scale, top = -500f * scale + (birdY * 20f * scale)) {
                    drawBird(scale, isDaytime)
                }
            }
        }
        
        // Info Card Overlay
        selectedBuilding?.let { building ->
            val l = profile.language
            val str = { fa: String, en: String -> if (l == "EN") en else fa }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier.padding(16.dp).glassmorphism(cornerRadius = 12.dp, surfaceColor = activeTheme.surfaceColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "${building.emoji} ${building.name}", color = activeTheme.primaryColor, fontWeight = FontWeight.Bold)
                        Text(text = str("نماد پایداری و پیشرفت شما! \nهزینه ساخت: ${building.cost} سکه", "Symbol of your progress!\nCost: ${building.cost} Coins"), color = activeTheme.fontColor, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

fun DrawScope.drawTree(scale: Float, idleAnimation: Float, isDaytime: Boolean) {
    val swing = (idleAnimation - 0.5f) * 10f * scale
    
    // Shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.2f),
        topLeft = Offset(-15f * scale, -5f * scale),
        size = Size(30f * scale, 10f * scale)
    )

    // Trunk
    val trunk = Path().apply {
        moveTo(-4f * scale, 0f)
        lineTo(4f * scale, 0f)
        lineTo(3f * scale + swing*0.1f, -15f * scale)
        lineTo(-3f * scale + swing*0.1f, -15f * scale)
        close()
    }
    drawPath(trunk, Color(0xFF5D4037).copy(alpha = if (isDaytime) 1f else 0.7f))
    
    // Layered Pine Leaves
    val leafColor1 = Color(0xFF2E7D32).copy(alpha = if (isDaytime) 1f else 0.8f)
    val leafColor2 = Color(0xFF388E3C).copy(alpha = if (isDaytime) 1f else 0.8f)
    val leafColor3 = Color(0xFF4CAF50).copy(alpha = if (isDaytime) 1f else 0.8f)

    val drawLayer = { width: Float, height: Float, yOffset: Float, color: Color ->
        val p = Path().apply {
            moveTo(swing, yOffset - height)
            lineTo(-width/2f + swing, yOffset)
            lineTo(width/2f + swing, yOffset)
            close()
        }
        drawPath(p, color)
    }

    drawLayer(40f * scale, 25f * scale, -10f * scale, leafColor1)
    drawLayer(30f * scale, 20f * scale, -20f * scale, leafColor2)
    drawLayer(20f * scale, 15f * scale, -30f * scale, leafColor3)
}

fun DrawScope.drawBird(scale: Float, isDaytime: Boolean) {
    val color = if (isDaytime) Color.DarkGray else Color.Black.copy(alpha=0.5f)
    val w = 15f * scale
    val birdPath = Path().apply {
        moveTo(0f, 0f)
        quadraticTo(w/2f, -w/2f, w, 0f)
        quadraticTo(w/2f, -w/4f, 0f, 0f)
        moveTo(0f, 0f)
        quadraticTo(-w/2f, -w/2f, -w, 0f)
        quadraticTo(-w/2f, -w/4f, 0f, 0f)
    }
    drawPath(birdPath, color)
}

fun DrawScope.drawCloud(scale: Float, color: Color) {
    val r = 20f * scale
    drawCircle(color, r, center = Offset(0f, 0f))
    drawCircle(color, r * 1.2f, center = Offset(r * 1.5f, -r * 0.5f))
    drawCircle(color, r * 0.9f, center = Offset(r * 3f, 0f))
}

fun DrawScope.drawIsometricTile(
    gridX: Float, gridY: Float,
    w: Float, h: Float,
    cTop: Color, cLeft: Color, cRight: Color
) {
    val pos = isoToScreen(gridX, gridY, 0f, w, h)
    val depth = 10f * (w / 100f) // tile thickness scaling
    translate(left = pos.x, top = pos.y) {
        // Tile top
        val pathTop = Path().apply {
            moveTo(0f, -h/2f)
            lineTo(w/2f, 0f)
            lineTo(0f, h/2f)
            lineTo(-w/2f, 0f)
            close()
        }
        
        // Tile left side
        val pathLeft = Path().apply {
            moveTo(-w/2f, 0f)
            lineTo(0f, h/2f)
            lineTo(0f, h/2f + depth)
            lineTo(-w/2f, depth)
            close()
        }
        
        // Tile right side
        val pathRight = Path().apply {
            moveTo(0f, h/2f)
            lineTo(w/2f, 0f)
            lineTo(w/2f, depth)
            lineTo(0f, h/2f + depth)
            close()
        }
        
        drawPath(pathLeft, cLeft)
        drawPath(pathRight, cRight)
        drawPath(pathTop, cTop)
        
        // Draw grid lines on top
        drawPath(pathTop, Color.Black.copy(alpha=0.05f), style = Stroke(width = 1f))
    }
}

fun DrawScope.drawHero(profile: UserProfile, scale: Float, idleAnimation: Float, isDaytime: Boolean) {
    val baseScale = (if (profile.level >= 10) 1.2f else 1f) * scale
    val bobbing = idleAnimation * (5f * baseScale)
    
    // Shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.25f),
        topLeft = Offset(-12f * baseScale, -5f * baseScale),
        size = Size(24f * baseScale, 10f * baseScale)
    )

    // Aura/Halo if level >= 5
    if (profile.level >= 5) {
        val auraColor = if (profile.level >= 20) Color(0xFFFFD700) else Color(0xFF64B5F6)
        drawCircle(
            color = auraColor.copy(alpha = 0.3f + (0.1f * idleAnimation)),
            radius = 20f * baseScale,
            center = Offset(0f, -25f * baseScale + bobbing)
        )
    }

    // Colors by class
    val bodyColor = when (profile.characterClass) {
        "WARRIOR" -> Color(0xFFD32F2F)
        "MAGE" -> Color(0xFF7B1FA2)
        "ROGUE" -> Color(0xFF388E3C)
        else -> Color.Gray
    }
    
    // Body (Capsule)
    val bodyPath = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(
            left = -10f * baseScale,
            top = -30f * baseScale + bobbing,
            right = 10f * baseScale,
            bottom = 0f + bobbing,
            radiusX = 10f * baseScale,
            radiusY = 10f * baseScale
        ))
    }
    drawPath(bodyPath, bodyColor.copy(alpha = if (isDaytime) 1f else 0.8f))
    
    // Head
    drawCircle(
        color = Color(0xFFFFCCBC).copy(alpha = if (isDaytime) 1f else 0.8f),
        radius = 8f * baseScale,
        center = Offset(0f, -35f * baseScale + bobbing)
    )

    // Weapon
    val weaponColor = Color(0xFFCFD8DC).copy(alpha = if (isDaytime) 1f else 0.8f)
    when (profile.characterClass) {
        "WARRIOR" -> { // Sword
            val sword = Path().apply {
                moveTo(12f * baseScale, -15f * baseScale + bobbing)
                lineTo(15f * baseScale, -35f * baseScale + bobbing)
                lineTo(9f * baseScale, -35f * baseScale + bobbing)
                close()
            }
            drawPath(sword, weaponColor)
        }
        "MAGE" -> { // Staff
            drawLine(
                color = Color(0xFF5D4037),
                start = Offset(12f * baseScale, 0f + bobbing),
                end = Offset(12f * baseScale, -40f * baseScale + bobbing),
                strokeWidth = 3f * baseScale
            )
            drawCircle(Color(0xFF00BCD4), 4f * baseScale, Offset(12f * baseScale, -40f * baseScale + bobbing))
        }
        "ROGUE" -> { // Daggers
            val dagger = Path().apply {
                moveTo(12f * baseScale, -10f * baseScale + bobbing)
                lineTo(14f * baseScale, -20f * baseScale + bobbing)
                lineTo(10f * baseScale, -20f * baseScale + bobbing)
                close()
            }
            drawPath(dagger, weaponColor)
        }
    }
}

fun DrawScope.drawBuilding(scale: Float, id: String, isDaytime: Boolean) {
    val h = (if (id == "TOWER" || id == "CASTLE") 80f else 40f) * scale
    val w = 60f * scale
    
    val baseColor = when (id) {
        "CASTLE" -> Color(0xFF78909C)
        "TOWER" -> Color(0xFF90A4AE)
        "GARDEN" -> Color(0xFF81C784)
        "LIBRARY" -> Color(0xFF8D6E63)
        "MARKET" -> Color(0xFFFFCA28)
        else -> Color.Gray
    }

    val leftColor = baseColor
    val rightColor = Color(
        red = baseColor.red * 0.8f,
        green = baseColor.green * 0.8f,
        blue = baseColor.blue * 0.8f
    )
    val topColor = Color(
        red = (baseColor.red * 1.2f).coerceAtMost(1f),
        green = (baseColor.green * 1.2f).coerceAtMost(1f),
        blue = (baseColor.blue * 1.2f).coerceAtMost(1f)
    )

    // Shadow
    val shadowPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(w*0.8f, -w/4f)
        lineTo(w*0.8f + h*0.5f, -w/4f - h*0.5f)
        lineTo(h*0.5f, -h*0.5f)
        close()
    }
    drawPath(shadowPath, Color.Black.copy(alpha = 0.15f))

    // Walls
    val leftWall = Path().apply {
        moveTo(0f, 0f)
        lineTo(-w/2f, -w/4f)
        lineTo(-w/2f, -w/4f - h)
        lineTo(0f, -h)
        close()
    }
    val rightWall = Path().apply {
        moveTo(0f, 0f)
        lineTo(w/2f, -w/4f)
        lineTo(w/2f, -w/4f - h)
        lineTo(0f, -h)
        close()
    }
    
    val globalAlpha = if (isDaytime) 1f else 0.7f
    drawPath(leftWall, leftColor.copy(alpha = globalAlpha))
    drawPath(rightWall, rightColor.copy(alpha = globalAlpha))
    
    // Roof types
    when (id) {
        "TOWER", "CASTLE" -> { // Battlements
            val roof = Path().apply {
                moveTo(0f, -h)
                lineTo(-w/2f, -w/4f - h)
                lineTo(0f, -h - w/4f)
                lineTo(w/2f, -w/4f - h)
                close()
            }
            drawPath(roof, topColor.copy(alpha = globalAlpha))
            
            // Draw simple door on left wall
            val door = Path().apply {
                moveTo(-w/8f, -w/16f)
                lineTo(-w/8f, -w/16f - 15f*scale)
                lineTo(-w/3f, -w/6f - 15f*scale)
                lineTo(-w/3f, -w/6f)
                close()
            }
            drawPath(door, Color(0xFF3E2723).copy(alpha = globalAlpha))

            if (!isDaytime) {
                drawCircle(Color(0xFFFFEA00), radius = 4f * scale, center = Offset(-w/4f, -h*0.7f))
                drawCircle(Color(0xFFFFEA00), radius = 4f * scale, center = Offset(w/4f, -h*0.7f))
            }
        }
        "LIBRARY", "MARKET" -> { // Sloped Roof
            val roofHeight = 25f * scale
            val roofFront = Path().apply {
                moveTo(0f, -h)
                lineTo(-w/2f, -w/4f - h)
                lineTo(0f, -h - roofHeight)
                close()
            }
            val roofSide = Path().apply {
                moveTo(0f, -h)
                lineTo(w/2f, -w/4f - h)
                lineTo(w/2f, -w/4f - h - roofHeight * 0.5f)
                lineTo(0f, -h - roofHeight)
                close()
            }
            val rColor = if (id == "MARKET") Color(0xFFE53935) else Color(0xFF3949AB)
            drawPath(roofFront, rColor.copy(alpha = globalAlpha))
            drawPath(roofSide, rColor.copy(alpha = globalAlpha * 0.8f))
        }
        "GARDEN" -> { // Flat top with bushes
            val roof = Path().apply {
                moveTo(0f, -h)
                lineTo(-w/2f, -w/4f - h)
                lineTo(0f, -h - w/4f)
                lineTo(w/2f, -w/4f - h)
                close()
            }
            drawPath(roof, Color(0xFF4CAF50).copy(alpha = globalAlpha))
            // Bushes
            drawCircle(Color(0xFF2E7D32).copy(alpha = globalAlpha), 10f*scale, Offset(-10f*scale, -h - 5f*scale))
            drawCircle(Color(0xFF1B5E20).copy(alpha = globalAlpha), 12f*scale, Offset(10f*scale, -h - 10f*scale))
            drawCircle(Color(0xFF81C784).copy(alpha = globalAlpha), 8f*scale, Offset(0f, -h + 5f*scale))
        }
    }
}
