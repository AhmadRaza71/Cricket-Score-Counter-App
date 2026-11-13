package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BoundaryPoint
import com.example.model.LandmarkMarker
import com.example.model.LandmarkType
import com.example.model.MatchEnvironment
import com.example.model.PitchMapConfig
import com.example.model.PitchSize
import com.example.ui.theme.BallAmber
import com.example.ui.theme.CricketLeatherRed
import com.example.ui.theme.TurfNeonGreen
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class CanvasInteractionMode(val label: String) {
    MOVE_PITCH("Move Pitch"),
    ADJUST_BOUNDARY("Boundary Pins"),
    DRAW_FREEHAND("Draw Boundary"),
    ADD_OBSTACLE("Add Obstacle")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PitchBoundaryCanvas(
    config: PitchMapConfig,
    onConfigChanged: (PitchMapConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeMode by remember { mutableStateOf(CanvasInteractionMode.ADJUST_BOUNDARY) }
    var activeMarkerForEdit by remember { mutableStateOf<LandmarkMarker?>(null) }
    var selectedLandmarkTypeToAdd by remember { mutableStateOf(LandmarkType.CAR) }

    // Dragging state
    var draggedPointIndex by remember { mutableIntStateOf(-1) }
    var draggedMarkerId by remember { mutableStateOf<String?>(null) }
    var isDraggingPitch by remember { mutableStateOf(false) }

    // Freehand drawing buffer
    var freehandPoints by remember { mutableStateOf<List<BoundaryPoint>>(emptyList()) }

    // Pitch Angle Slider dialog
    var showRotationControls by remember { mutableStateOf(false) }

    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier.fillMaxSize()) {
        // 1. TOP TOOLBAR: Interaction Mode Tabs
        TabRow(selectedTabIndex = activeMode.ordinal) {
            CanvasInteractionMode.values().forEach { mode ->
                Tab(
                    selected = activeMode == mode,
                    onClick = { activeMode = mode },
                    text = { Text(mode.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    icon = {
                        Icon(
                            when (mode) {
                                CanvasInteractionMode.MOVE_PITCH -> Icons.Default.OpenWith
                                CanvasInteractionMode.ADJUST_BOUNDARY -> Icons.Default.FormatShapes
                                CanvasInteractionMode.DRAW_FREEHAND -> Icons.Default.Draw
                                CanvasInteractionMode.ADD_OBSTACLE -> Icons.Default.LocationOn
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        // 2. CONTEXTUAL MODE TOOLBAR & DIMENSIONS BANNER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (activeMode) {
                        CanvasInteractionMode.MOVE_PITCH -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pitch Controls:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.width(8.dp))
                                PitchSize.values().forEach { ps ->
                                    FilterChip(
                                        selected = config.pitchSize == ps,
                                        onClick = { onConfigChanged(config.copy(pitchSize = ps)) },
                                        label = { Text(ps.displayName, fontSize = 11.sp) },
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { showRotationControls = !showRotationControls }) {
                                Icon(Icons.Default.RotateRight, contentDescription = "Rotate Pitch", tint = TurfNeonGreen)
                            }
                        }

                        CanvasInteractionMode.ADJUST_BOUNDARY -> {
                            Text(
                                "Drag any corner node to reshape the boundary.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = {
                                        // Narrow Gali preset
                                        onConfigChanged(
                                            config.copy(
                                                boundaryPolygon = listOf(
                                                    BoundaryPoint(0.25f, 0.05f),
                                                    BoundaryPoint(0.75f, 0.05f),
                                                    BoundaryPoint(0.75f, 0.95f),
                                                    BoundaryPoint(0.25f, 0.95f)
                                                )
                                            )
                                        )
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Gali Shape", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        // Square roof preset
                                        onConfigChanged(
                                            config.copy(
                                                boundaryPolygon = listOf(
                                                    BoundaryPoint(0.12f, 0.12f),
                                                    BoundaryPoint(0.88f, 0.12f),
                                                    BoundaryPoint(0.88f, 0.88f),
                                                    BoundaryPoint(0.12f, 0.88f)
                                                )
                                            )
                                        )
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Rooftop", fontSize = 11.sp)
                                }
                            }
                        }

                        CanvasInteractionMode.DRAW_FREEHAND -> {
                            Text(
                                "Draw a continuous boundary curve with your finger.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = { freehandPoints = emptyList() },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Clear Trace", fontSize = 11.sp)
                            }
                        }

                        CanvasInteractionMode.ADD_OBSTACLE -> {
                            Text("Select obstacle to place:", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                LandmarkType.values().take(4).forEach { lt ->
                                    FilterChip(
                                        selected = selectedLandmarkTypeToAdd == lt,
                                        onClick = { selectedLandmarkTypeToAdd = lt },
                                        label = { Text(lt.displayName, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Pitch Rotation Slider (if opened in Move Pitch mode)
                if (activeMode == CanvasInteractionMode.MOVE_PITCH && showRotationControls) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Angle: ${config.pitchRotation.roundToInt()}°", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = config.pitchRotation,
                            onValueChange = { onConfigChanged(config.copy(pitchRotation = it)) },
                            valueRange = 0f..360f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. THE GESTURE-POWERED CANVAS VIEWPORT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .background(
                    when (config.environment) {
                        MatchEnvironment.STREET -> Color(0xFF161C22)
                        MatchEnvironment.ROAD -> Color(0xFF11171C)
                        MatchEnvironment.ROOF -> Color(0xFF22262C)
                        MatchEnvironment.GROUND -> Color(0xFF0F2617)
                    },
                    RoundedCornerShape(16.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activeMode, config) {
                        detectTapGestures { tapOffset ->
                            val normX = tapOffset.x / size.width
                            val normY = tapOffset.y / size.height

                            when (activeMode) {
                                CanvasInteractionMode.ADD_OBSTACLE -> {
                                    // Check if tapping existing obstacle
                                    val hitLm = config.landmarks.firstOrNull { lm ->
                                        hypot(lm.x * size.width - tapOffset.x, lm.y * size.height - tapOffset.y) < 50f
                                    }
                                    if (hitLm != null) {
                                        activeMarkerForEdit = hitLm
                                    } else {
                                        val newMarker = LandmarkMarker(
                                            name = selectedLandmarkTypeToAdd.displayName,
                                            x = normX.coerceIn(0.05f, 0.95f),
                                            y = normY.coerceIn(0.05f, 0.95f),
                                            type = selectedLandmarkTypeToAdd,
                                            penaltyRuns = selectedLandmarkTypeToAdd.defaultPenalty,
                                            isOut = selectedLandmarkTypeToAdd.defaultIsOut
                                        )
                                        onConfigChanged(config.copy(landmarks = config.landmarks + newMarker))
                                    }
                                }

                                CanvasInteractionMode.MOVE_PITCH -> {
                                    // Tap to place pitch center
                                    onConfigChanged(
                                        config.copy(
                                            pitchCenterX = normX.coerceIn(0.2f, 0.8f),
                                            pitchCenterY = normY.coerceIn(0.2f, 0.8f)
                                        )
                                    )
                                }

                                CanvasInteractionMode.ADJUST_BOUNDARY -> {
                                    // Check if tapped near an existing vertex to delete (if > 3 points)
                                    val hitVertex = config.boundaryPolygon.indexOfFirst { pt ->
                                        hypot(pt.x * size.width - tapOffset.x, pt.y * size.height - tapOffset.y) < 40f
                                    }
                                    if (hitVertex >= 0 && config.boundaryPolygon.size > 3) {
                                        // Tap on vertex deletes it if user wants
                                    } else {
                                        // Tap on open ground adds a corner node
                                        if (config.boundaryPolygon.size < 12) {
                                            val newPt = BoundaryPoint(normX, normY)
                                            onConfigChanged(config.copy(boundaryPolygon = config.boundaryPolygon + newPt))
                                        }
                                    }
                                }

                                CanvasInteractionMode.DRAW_FREEHAND -> {
                                    // Freehand handled via drag
                                }
                            }
                        }
                    }
                    .pointerInput(activeMode, config) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val w = size.width
                                val h = size.height

                                when (activeMode) {
                                    CanvasInteractionMode.MOVE_PITCH -> {
                                        val pX = config.pitchCenterX * w
                                        val pY = config.pitchCenterY * h
                                        if (hypot(pX - startOffset.x, pY - startOffset.y) < 120f) {
                                            isDraggingPitch = true
                                        }
                                    }

                                    CanvasInteractionMode.ADJUST_BOUNDARY -> {
                                        val hitVertex = config.boundaryPolygon.indexOfFirst { pt ->
                                            hypot(pt.x * w - startOffset.x, pt.y * h - startOffset.y) < 55f
                                        }
                                        if (hitVertex >= 0) {
                                            draggedPointIndex = hitVertex
                                        }
                                    }

                                    CanvasInteractionMode.DRAW_FREEHAND -> {
                                        freehandPoints = listOf(BoundaryPoint(startOffset.x / w, startOffset.y / h))
                                    }

                                    CanvasInteractionMode.ADD_OBSTACLE -> {
                                        val hitLm = config.landmarks.firstOrNull { lm ->
                                            hypot(lm.x * w - startOffset.x, lm.y * h - startOffset.y) < 55f
                                        }
                                        draggedMarkerId = hitLm?.id
                                    }
                                }
                            },
                            onDragEnd = {
                                if (activeMode == CanvasInteractionMode.DRAW_FREEHAND && freehandPoints.size >= 4) {
                                    // Resample freehand points into a clean 6-8 point polygon
                                    val step = max(1, freehandPoints.size / 8)
                                    val sampled = freehandPoints.filterIndexed { index, _ -> index % step == 0 }
                                    if (sampled.size >= 3) {
                                        onConfigChanged(config.copy(boundaryPolygon = sampled))
                                    }
                                    freehandPoints = emptyList()
                                }
                                draggedPointIndex = -1
                                draggedMarkerId = null
                                isDraggingPitch = false
                            },
                            onDragCancel = {
                                draggedPointIndex = -1
                                draggedMarkerId = null
                                isDraggingPitch = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val w = size.width
                                val h = size.height

                                when (activeMode) {
                                    CanvasInteractionMode.MOVE_PITCH -> {
                                        if (isDraggingPitch) {
                                            val newX = (config.pitchCenterX + dragAmount.x / w).coerceIn(0.15f, 0.85f)
                                            val newY = (config.pitchCenterY + dragAmount.y / h).coerceIn(0.15f, 0.85f)
                                            onConfigChanged(config.copy(pitchCenterX = newX, pitchCenterY = newY))
                                        }
                                    }

                                    CanvasInteractionMode.ADJUST_BOUNDARY -> {
                                        if (draggedPointIndex in config.boundaryPolygon.indices) {
                                            val current = config.boundaryPolygon[draggedPointIndex]
                                            val newX = (current.x + dragAmount.x / w).coerceIn(0.02f, 0.98f)
                                            val newY = (current.y + dragAmount.y / h).coerceIn(0.02f, 0.98f)
                                            val updatedList = config.boundaryPolygon.toMutableList()
                                            updatedList[draggedPointIndex] = BoundaryPoint(newX, newY)
                                            onConfigChanged(config.copy(boundaryPolygon = updatedList))
                                        }
                                    }

                                    CanvasInteractionMode.DRAW_FREEHAND -> {
                                        val normX = (change.position.x / w).coerceIn(0.02f, 0.98f)
                                        val normY = (change.position.y / h).coerceIn(0.02f, 0.98f)
                                        freehandPoints = freehandPoints + BoundaryPoint(normX, normY)
                                    }

                                    CanvasInteractionMode.ADD_OBSTACLE -> {
                                        if (draggedMarkerId != null) {
                                            val updatedLms = config.landmarks.map { lm ->
                                                if (lm.id == draggedMarkerId) {
                                                    val newX = (lm.x + dragAmount.x / w).coerceIn(0.05f, 0.95f)
                                                    val newY = (lm.y + dragAmount.y / h).coerceIn(0.05f, 0.95f)
                                                    lm.copy(x = newX, y = newY)
                                                } else lm
                                            }
                                            onConfigChanged(config.copy(landmarks = updatedLms))
                                        }
                                    }
                                }
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw Environment Backdrop Grid / Street Markings
                drawGroundEnvironmentDetails(config.environment, w, h)

                // Draw Boundary Polygon
                val polygonPoints = if (activeMode == CanvasInteractionMode.DRAW_FREEHAND && freehandPoints.size >= 2) {
                    freehandPoints
                } else {
                    config.boundaryPolygon
                }

                if (polygonPoints.size >= 3) {
                    val polyPath = Path()
                    val firstPt = polygonPoints.first()
                    polyPath.moveTo(firstPt.x * w, firstPt.y * h)
                    for (i in 1 until polygonPoints.size) {
                        val pt = polygonPoints[i]
                        polyPath.lineTo(pt.x * w, pt.y * h)
                    }
                    polyPath.close()

                    // Glow fill
                    drawPath(
                        path = polyPath,
                        color = TurfNeonGreen.copy(alpha = 0.08f)
                    )

                    // Dashed boundary fence line
                    drawPath(
                        path = polyPath,
                        color = TurfNeonGreen,
                        style = Stroke(
                            width = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                        )
                    )

                    // Draw corner handle rings (if in vertex mode)
                    if (activeMode == CanvasInteractionMode.ADJUST_BOUNDARY) {
                        config.boundaryPolygon.forEachIndexed { idx, pt ->
                            val px = pt.x * w
                            val py = pt.y * h
                            val isSelected = (idx == draggedPointIndex)
                            drawCircle(
                                color = if (isSelected) Color(0xFFFFD700) else TurfNeonGreen,
                                radius = if (isSelected) 20f else 15f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 7f,
                                center = Offset(px, py)
                            )
                        }
                    }
                }

                // Calculate & Draw Dimension Guide Lines from Pitch to Boundaries
                val pitchCenterPx = Offset(config.pitchCenterX * w, config.pitchCenterY * h)
                drawDimensionGuideLines(pitchCenterPx, config.boundaryPolygon, w, h, textMeasurer)

                // Draw Pitch Marker & Stumps with Rotation & Gestures
                rotate(degrees = config.pitchRotation, pivot = pitchCenterPx) {
                    drawInteractivePitch(config, pitchCenterPx, w, h)
                }

                // Draw Landmark Obstacle Pins
                config.landmarks.forEach { lm ->
                    val lmX = lm.x * w
                    val lmY = lm.y * h
                    val markerColor = if (lm.isOut) CricketLeatherRed else Color(0xFFFFB300)

                    // Outer pulse
                    drawCircle(color = markerColor.copy(alpha = 0.35f), radius = 26f, center = Offset(lmX, lmY))
                    // Solid circle
                    drawCircle(color = markerColor, radius = 16f, center = Offset(lmX, lmY))
                    // Inner dot
                    drawCircle(color = Color.White, radius = 6f, center = Offset(lmX, lmY))

                    // Draw label
                    val textResult = textMeasurer.measure(
                        text = lm.name,
                        style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset(lmX - textResult.size.width / 2f, lmY + 18f)
                    )
                }
            }

            // Compass / Direction Indicator
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape
            ) {
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Text("N ↑", color = TurfNeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        // 4. BOTTOM DIMENSIONS SUMMARY CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Off-Boundary", style = MaterialTheme.typography.labelSmall)
                    Text("~16m", fontWeight = FontWeight.Bold, color = TurfNeonGreen, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Leg-Boundary", style = MaterialTheme.typography.labelSmall)
                    Text("~14m", fontWeight = FontWeight.Bold, color = TurfNeonGreen, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Straight Drive", style = MaterialTheme.typography.labelSmall)
                    Text("~26m", fontWeight = FontWeight.Bold, color = TurfNeonGreen, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Area Size", style = MaterialTheme.typography.labelSmall)
                    Text("~380 m²", fontWeight = FontWeight.Bold, color = BallAmber, fontSize = 13.sp)
                }
            }
        }
    }

    // Landmark Edit Dialog
    activeMarkerForEdit?.let { marker ->
        AlertDialog(
            onDismissRequest = { activeMarkerForEdit = null },
            title = {
                Text(text = "Obstacle: ${marker.name}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Customize rules for when the ball hits this obstacle:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dismissal (Ball hits = OUT):")
                        Switch(
                            checked = marker.isOut,
                            onCheckedChange = { checked ->
                                val updated = marker.copy(isOut = checked)
                                activeMarkerForEdit = updated
                                onConfigChanged(
                                    config.copy(
                                        landmarks = config.landmarks.map { if (it.id == marker.id) updated else it }
                                    )
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Penalty Runs:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    val updated = marker.copy(penaltyRuns = marker.penaltyRuns - 1)
                                    activeMarkerForEdit = updated
                                    onConfigChanged(
                                        config.copy(
                                            landmarks = config.landmarks.map { if (it.id == marker.id) updated else it }
                                        )
                                    )
                                }
                            ) { Text("-1") }
                            Text(
                                "${marker.penaltyRuns}",
                                modifier = Modifier.padding(horizontal = 12.dp),
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = {
                                    val updated = marker.copy(penaltyRuns = marker.penaltyRuns + 1)
                                    activeMarkerForEdit = updated
                                    onConfigChanged(
                                        config.copy(
                                            landmarks = config.landmarks.map { if (it.id == marker.id) updated else it }
                                        )
                                    )
                                }
                            ) { Text("+1") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeMarkerForEdit = null }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onConfigChanged(
                            config.copy(landmarks = config.landmarks.filter { it.id != marker.id })
                        )
                        activeMarkerForEdit = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CricketLeatherRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Obstacle")
                }
            }
        )
    }
}

private fun DrawScope.drawGroundEnvironmentDetails(environment: MatchEnvironment, w: Float, h: Float) {
    when (environment) {
        MatchEnvironment.ROAD -> {
            // Road center dashed lines
            val dashPath = Path().apply {
                moveTo(w / 2f, 0f)
                lineTo(w / 2f, h)
            }
            drawPath(
                path = dashPath,
                color = Color.Yellow.copy(alpha = 0.25f),
                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f), 0f))
            )
        }
        MatchEnvironment.STREET -> {
            // Alley sidewalk borders
            drawLine(Color.White.copy(alpha = 0.15f), Offset(w * 0.15f, 0f), Offset(w * 0.15f, h), strokeWidth = 2f)
            drawLine(Color.White.copy(alpha = 0.15f), Offset(w * 0.85f, 0f), Offset(w * 0.85f, h), strokeWidth = 2f)
        }
        MatchEnvironment.ROOF -> {
            // Rooftop boundary edge outline
            drawRect(Color.White.copy(alpha = 0.1f), Offset(w * 0.08f, h * 0.08f), Size(w * 0.84f, h * 0.84f), style = Stroke(width = 2f))
        }
        MatchEnvironment.GROUND -> {
            // Cut grass mowing circles
            drawCircle(Color.White.copy(alpha = 0.05f), radius = w * 0.35f, center = Offset(w / 2f, h / 2f), style = Stroke(width = 1f))
        }
    }
}

private fun DrawScope.drawInteractivePitch(config: PitchMapConfig, center: Offset, w: Float, h: Float) {
    val pitchW = when (config.pitchSize) {
        PitchSize.SHORT -> w * 0.16f
        PitchSize.STANDARD -> w * 0.22f
        PitchSize.LONG -> w * 0.28f
    }
    val pitchH = h * 0.42f
    val pitchLeft = center.x - pitchW / 2f
    val pitchTop = center.y - pitchH / 2f

    // Pitch turf / matting rectangle
    drawRoundRect(
        color = Color(0xFFC2B280).copy(alpha = 0.85f),
        topLeft = Offset(pitchLeft, pitchTop),
        size = Size(pitchW, pitchH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
    )

    // Crease Lines (Bowling crease & Batting crease)
    val bowlingCreaseY = pitchTop + pitchH * 0.12f
    val battingCreaseY = pitchTop + pitchH * 0.88f
    drawLine(color = Color.White, start = Offset(pitchLeft, bowlingCreaseY), end = Offset(pitchLeft + pitchW, bowlingCreaseY), strokeWidth = 3f)
    drawLine(color = Color.White, start = Offset(pitchLeft, battingCreaseY), end = Offset(pitchLeft + pitchW, battingCreaseY), strokeWidth = 3f)

    // Stumps (Strikers end & Non-strikers end)
    for (s in 0..2) {
        val stumpSpacing = pitchW / 4f
        val stumpX = pitchLeft + stumpSpacing * (s + 1)
        // Bowler end stumps
        drawCircle(color = Color(0xFF8D6E63), radius = 4f, center = Offset(stumpX, pitchTop + pitchH * 0.08f))
        // Batter end stumps
        drawCircle(color = Color(0xFF8D6E63), radius = 4f, center = Offset(stumpX, pitchTop + pitchH * 0.92f))
    }

    // Striker Crease "BAT" label
    drawCircle(color = TurfNeonGreen, radius = 6f, center = Offset(center.x, battingCreaseY + 12f))
}

private fun DrawScope.drawDimensionGuideLines(
    pitchCenter: Offset,
    polygon: List<BoundaryPoint>,
    w: Float,
    h: Float,
    measurer: androidx.compose.ui.text.TextMeasurer
) {
    if (polygon.size < 3) return

    // Draw thin distance indicator rays from pitch center to cardinal directions
    val directions = listOf(
        Pair("Straight (26m)", Offset(pitchCenter.x, 0.06f * h)),
        Pair("Behind (10m)", Offset(pitchCenter.x, 0.94f * h)),
        Pair("Offside (16m)", Offset(0.06f * w, pitchCenter.y)),
        Pair("Legside (14m)", Offset(0.94f * w, pitchCenter.y))
    )

    directions.forEach { (label, target) ->
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = pitchCenter,
            end = target,
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    }
}
