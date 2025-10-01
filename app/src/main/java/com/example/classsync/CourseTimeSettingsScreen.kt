package com.example.classsync

import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState // Added
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll // Added
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.animation.core.animateFloatAsState
import com.example.classsync.ui.theme.*

// SharedPreferences Constants
private const val PREFS_NAME = "ClassSyncSettings"
private const val KEY_LESSON_DURATION = "lessonDuration"
private const val KEY_BREAK_DURATION = "breakDuration"
private const val KEY_MORNING_LESSONS = "morningLessons"
private const val KEY_AFTERNOON_LESSONS = "afternoonLessons"
private const val KEY_EVENING_LESSONS = "eveningLessons"

// 定义课程时间数据模型
data class LessonTime(
    val lessonNumber: Int,
    var startTime: String,
    var endTime: String
)

// Helper function to serialize List<LessonTime> to String
private fun serializeLessonTimes(lessons: List<LessonTime>): String {
    return lessons.joinToString(";") { "${it.lessonNumber},${it.startTime},${it.endTime}" }
}

// Helper function to deserialize String to SnapshotStateList<LessonTime>
private fun deserializeLessonTimes(data: String?): SnapshotStateList<LessonTime> {
    val list = mutableStateListOf<LessonTime>()
    data?.takeIf { it.isNotBlank() }?.split(";")?.forEach {
        try {
            val parts = it.split(",")
            if (parts.size == 3) {
                list.add(LessonTime(parts[0].toInt(), parts[1], parts[2]))
            }
        } catch (e: Exception) {
            Log.e("DeserializeError", "Error parsing lesson time: $it", e)
        }
    }
    return list
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTimeSettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var lessonDuration by remember { 
        mutableStateOf(sharedPreferences.getString(KEY_LESSON_DURATION, "45分钟") ?: "45分钟") 
    }
    var breakDuration by remember { 
        mutableStateOf(sharedPreferences.getString(KEY_BREAK_DURATION, "10分钟") ?: "10分钟") 
    }
    
    val morningLessons = remember { deserializeLessonTimes(sharedPreferences.getString(KEY_MORNING_LESSONS, null)) }
    val afternoonLessons = remember { deserializeLessonTimes(sharedPreferences.getString(KEY_AFTERNOON_LESSONS, null)) }
    val eveningLessons = remember { deserializeLessonTimes(sharedPreferences.getString(KEY_EVENING_LESSONS, null)) }

    val allBlocks = remember(morningLessons, afternoonLessons, eveningLessons) {
        listOf(morningLessons, afternoonLessons, eveningLessons)
    }
    
    val scrollState = rememberScrollState() // Added scrollState

    // Save data whenever it changes
    LaunchedEffect(lessonDuration) {
        with(sharedPreferences.edit()) {
            putString(KEY_LESSON_DURATION, lessonDuration)
            apply()
        }
    }
    LaunchedEffect(breakDuration) {
        with(sharedPreferences.edit()) {
            putString(KEY_BREAK_DURATION, breakDuration)
            apply()
        }
    }
    LaunchedEffect(morningLessons.toList()) { // Observe changes to the list content
        with(sharedPreferences.edit()) {
            putString(KEY_MORNING_LESSONS, serializeLessonTimes(morningLessons))
            apply()
        }
    }
    LaunchedEffect(afternoonLessons.toList()) {
        with(sharedPreferences.edit()) {
            putString(KEY_AFTERNOON_LESSONS, serializeLessonTimes(afternoonLessons))
            apply()
        }
    }
    LaunchedEffect(eveningLessons.toList()) {
        with(sharedPreferences.edit()) {
            putString(KEY_EVENING_LESSONS, serializeLessonTimes(eveningLessons))
            apply()
        }
    }

    var showLessonDurationDialog by remember { mutableStateOf(false) }
    var showBreakDurationDialog by remember { mutableStateOf(false) }
    var showLessonTimeEditDialog by remember { mutableStateOf(false) }
    var editingLesson by remember { mutableStateOf<LessonTime?>(null) }
    var editingLessonBlockTitle by remember { mutableStateOf<String?>(null) }
    
    val interactionSource = remember { MutableInteractionSource() }

    val isAnyDialogShown by remember {
        derivedStateOf { showLessonDurationDialog || showBreakDurationDialog || showLessonTimeEditDialog }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(GradientBlue, GradientPurple), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)))
    ) { 
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isAnyDialogShown) Modifier.blur(radius = 16.dp) else Modifier), 
            containerColor = Color.Transparent, 
            topBar = {
                TopAppBar(
                    title = { Text("课程时间设置", color = Color.White) },
                    navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White) } },
                    actions = {
                        Button(onClick = { onNavigateBack() }, modifier = Modifier.padding(end = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = PurpleBlueAccent), shape = RoundedCornerShape(8.dp)) {
                            Text("完成", color = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(scrollState) // Added verticalScroll
            ) {
                SettingCard {
                    SettingItem(title = "每节课上课时长", value = lessonDuration, onClick = { showLessonDurationDialog = true })
                    Divider(color = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingItem(title = "课间休息时长", value = breakDuration, onClick = { showBreakDurationDialog = true })
                }
                Spacer(Modifier.height(16.dp))
                LessonBlockCard("上午课程", morningLessons, { adjustLessonCount(morningLessons, it, lessonDuration, breakDuration, allBlocks) }, { lesson, blockTitle -> editingLesson = lesson; editingLessonBlockTitle = blockTitle; showLessonTimeEditDialog = true })
                Spacer(Modifier.height(16.dp))
                LessonBlockCard("下午课程", afternoonLessons, { adjustLessonCount(afternoonLessons, it, lessonDuration, breakDuration, allBlocks) }, { lesson, blockTitle -> editingLesson = lesson; editingLessonBlockTitle = blockTitle; showLessonTimeEditDialog = true })
                Spacer(Modifier.height(16.dp))
                LessonBlockCard("晚上课程", eveningLessons, { adjustLessonCount(eveningLessons, it, lessonDuration, breakDuration, allBlocks) }, { lesson, blockTitle -> editingLesson = lesson; editingLessonBlockTitle = blockTitle; showLessonTimeEditDialog = true })
                 Spacer(Modifier.height(16.dp)) // Add some space at the bottom for better scroll visibility if needed
            }
        }

        if (showLessonDurationDialog) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = interactionSource, indication = null) { showLessonDurationDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.padding(horizontal = 32.dp).clickable(interactionSource = interactionSource, indication = null) {}) { 
                    val currentDurationMinutes = lessonDuration.filter { it.isDigit() }.toIntOrNull() ?: 45
                    DurationPickerContent("每节课上课时长", "将自动调整所有课程的开始及结束时间", currentDurationMinutes, (20..90 step 5).toList(), { selectedMinutes ->
                        lessonDuration = "${selectedMinutes}分钟"
                        updateAllLessonTimes(allBlocks, lessonDuration, breakDuration, newLessonDuration = "${selectedMinutes}分钟")
                        showLessonDurationDialog = false
                    }, { showLessonDurationDialog = false })
                }
            }
        }

        if (showBreakDurationDialog) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = interactionSource, indication = null) { showBreakDurationDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.padding(horizontal = 32.dp).clickable(interactionSource = interactionSource, indication = null) {}) { 
                    val currentDurationMinutes = breakDuration.filter { it.isDigit() }.toIntOrNull() ?: 10
                    DurationPickerContent("课间休息时长", "将自动调整所有课程的开始及结束时间", currentDurationMinutes, (0..30 step 5).toList(), { selectedMinutes ->
                        breakDuration = "${selectedMinutes}分钟"
                        updateAllLessonTimes(allBlocks, lessonDuration, breakDuration, newBreakDuration = "${selectedMinutes}分钟")
                        showBreakDurationDialog = false
                    }, { showBreakDurationDialog = false })
                }
            }
        }

        if (showLessonTimeEditDialog && editingLesson != null && editingLessonBlockTitle != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = interactionSource, indication = null) { showLessonTimeEditDialog = false },
                contentAlignment = Alignment.Center
            ) {
                 Box(modifier = Modifier.padding(horizontal = 32.dp).clickable(interactionSource = interactionSource, indication = null) {}) {
                    val lessonToEdit = editingLesson!!
                    val blockTitle = editingLessonBlockTitle!!
                    val currentLessonDurationMinutes = lessonDuration.filter { it.isDigit() }.toIntOrNull() ?: 45
                    LessonTimePickerContent(lessonToEdit.lessonNumber, blockTitle, lessonToEdit.startTime, currentLessonDurationMinutes, 
                        onDismiss = { showLessonTimeEditDialog = false }, 
                        onConfirm = { newStartHour, newStartMinute ->
                            val newStartTimeString = String.format("%02d:%02d", newStartHour, newStartMinute)
                            // Find the lesson and update it by creating a new instance for Compose to detect
                            val targetList = when(blockTitle) {
                                "上午课程" -> morningLessons
                                "下午课程" -> afternoonLessons
                                "晚上课程" -> eveningLessons
                                else -> null
                            }
                            targetList?.let {
                                val lessonIndex = it.indexOf(lessonToEdit)
                                if (lessonIndex != -1) {
                                    it[lessonIndex] = lessonToEdit.copy(startTime = newStartTimeString)
                                    // Get the updated lesson for updateAllLessonTimes
                                    val updatedLesson = it[lessonIndex]
                                    val editedBlockIndex = allBlocks.indexOf(targetList)
                                    updateAllLessonTimes(allBlocks, lessonDuration, breakDuration, startingFromLesson = updatedLesson, startBlockIndex = editedBlockIndex, startLessonIndexInBlock = lessonIndex)
                                }
                            }
                            showLessonTimeEditDialog = false
                        }
                    )
                }
            }
        }
    }
}

private fun updateAllLessonTimes(
    allLessons: List<SnapshotStateList<LessonTime>>,
    lessonDurationStr: String,
    breakDurationStr: String,
    newLessonDuration: String? = null,
    newBreakDuration: String? = null,
    startingFromLesson: LessonTime? = null,
    startBlockIndex: Int = 0,
    startLessonIndexInBlock: Int = 0
) {
    val actualLessonDurationMinutes = (newLessonDuration ?: lessonDurationStr).filter { it.isDigit() }.toIntOrNull() ?: 45
    val actualBreakDurationMinutes = (newBreakDuration ?: breakDurationStr).filter { it.isDigit() }.toIntOrNull() ?: 10

    var previousLessonEndTimeTotalMinutes = -1

    if (startingFromLesson != null) {
        val startMinutes = timeToMinutes(startingFromLesson.startTime)
        previousLessonEndTimeTotalMinutes = startMinutes + actualLessonDurationMinutes
        startingFromLesson.endTime = minutesToTime(previousLessonEndTimeTotalMinutes) // Update the end time of the manually edited lesson
    }

    var firstLessonInProcessingBlock = true

    for (currentBlockIdx in allLessons.indices) {
        if (startingFromLesson != null && currentBlockIdx < startBlockIndex) continue
        val lessonBlock = allLessons[currentBlockIdx]
        
        // Determine if this block is the one where editing started
        val isStartingBlock = (startingFromLesson != null && currentBlockIdx == startBlockIndex)
        firstLessonInProcessingBlock = true // Reset for each block unless it's the starting block and we are past the edited item

        for (currentLessonIdxInBlock in lessonBlock.indices) {
            if (isStartingBlock) {
                if (currentLessonIdxInBlock < startLessonIndexInBlock) continue // Skip lessons before the edited one in the starting block
                if (lessonBlock[currentLessonIdxInBlock] === startingFromLesson || currentLessonIdxInBlock == startLessonIndexInBlock) { // The edited lesson or its new instance
                    // This lesson's start and end times are already set (or being set if it's the startingFromLesson itself)
                    // We use its already calculated previousLessonEndTimeTotalMinutes for the next one.
                    firstLessonInProcessingBlock = false // The next lesson in this block will use the break
                    continue 
                }
            }

            val lesson = lessonBlock[currentLessonIdxInBlock]
            val currentLessonStartTimeTotalMinutes: Int

            if (firstLessonInProcessingBlock && previousLessonEndTimeTotalMinutes == -1 && startingFromLesson == null) {
                 // This is the very first lesson of a block, and no specific lesson was edited to start from,
                // or we are processing blocks after the edited one.
                // It should respect its original start time or a default block start time if list was empty.
                 val blockBaseTime = when (lessonBlock) {
                     allLessons.getOrNull(0) -> "08:00" 
                     allLessons.getOrNull(1) -> allLessons.getOrNull(0)?.lastOrNull()?.endTime?.let { minutesToTime(timeToMinutes(it) + actualBreakDurationMinutes) } ?: "13:00"
                     allLessons.getOrNull(2) -> allLessons.getOrNull(1)?.lastOrNull()?.endTime?.let { minutesToTime(timeToMinutes(it) + actualBreakDurationMinutes) } ?: "18:00"
                     else -> "08:00"
                 }
                 currentLessonStartTimeTotalMinutes = timeToMinutes(if(lessonBlock.size == 1 && startingFromLesson == null) blockBaseTime else lesson.startTime)            
            } else if (firstLessonInProcessingBlock && previousLessonEndTimeTotalMinutes != -1) {
                // This is the first lesson in a new block, following a block that had lessons
                // (could be the block after the one with the startingFromLesson, or a global update starting a new block)
                currentLessonStartTimeTotalMinutes = previousLessonEndTimeTotalMinutes + actualBreakDurationMinutes
            } else if (previousLessonEndTimeTotalMinutes == -1) {
                // This case should ideally not be hit if logic is correct, implies a fresh start mid-way without context.
                // Defaulting to a sensible start for the block.
                 val blockBaseTime = when (lessonBlock) {
                     allLessons.getOrNull(0) -> "08:00" 
                     allLessons.getOrNull(1) -> allLessons.getOrNull(0)?.lastOrNull()?.endTime?.let { minutesToTime(timeToMinutes(it) + actualBreakDurationMinutes) } ?: "13:00"
                     allLessons.getOrNull(2) -> allLessons.getOrNull(1)?.lastOrNull()?.endTime?.let { minutesToTime(timeToMinutes(it) + actualBreakDurationMinutes) } ?: "18:00"
                     else -> "08:00"
                 }
                currentLessonStartTimeTotalMinutes = timeToMinutes(blockBaseTime)
                Log.d("UpdateTimes", "Fallback for block start time for lesson ${lesson.lessonNumber}")
            } else {
                // Standard case: subsequent lesson in a block
                currentLessonStartTimeTotalMinutes = previousLessonEndTimeTotalMinutes + actualBreakDurationMinutes
            }

            val newStartTime = minutesToTime(currentLessonStartTimeTotalMinutes)
            previousLessonEndTimeTotalMinutes = currentLessonStartTimeTotalMinutes + actualLessonDurationMinutes
            val newEndTime = minutesToTime(previousLessonEndTimeTotalMinutes)

            // 使用 copy() 创建新实例并替换旧实例，以确保UI正确重组
            lessonBlock[currentLessonIdxInBlock] = lesson.copy(
                startTime = newStartTime,
                endTime = newEndTime
            )
            firstLessonInProcessingBlock = false
        }
        // If this block was not the starting block and did not contain the starting lesson, 
        // or if it was a global update (startingFromLesson == null), 
        // and the block is not empty, reset previousLessonEndTimeTotalMinutes for the next block (unless it's the last block).
        if (startingFromLesson == null && lessonBlock.isNotEmpty()) {
             // For global updates, previous time is carried to next block's first lesson calculation (via break).
             // No reset needed here, it's handled by firstLessonInProcessingBlock logic for block base time.
        } else if (isStartingBlock && lessonBlock.isNotEmpty()) {
            // If it was the starting block, previousLessonEndTimeTotalMinutes is correctly set from its last lesson.
        } else if (lessonBlock.isNotEmpty()){
            // A block fully after the startingFromLesson block, or just some other block during a global update.
            // The previousLessonEndTimeTotalMinutes is correctly set by its last lesson.
        }
    }
}


private fun timeToMinutes(time: String): Int {
    return try { val parts = time.split(":"); parts[0].toInt() * 60 + parts[1].toInt() } catch (e: Exception) { 0 }
}
private fun minutesToTime(totalMinutes: Int): String {
    val hours = (totalMinutes / 60) % 24; val minutes = totalMinutes % 60; return String.format("%02d:%02d", hours, minutes)
}

private fun adjustLessonCount(
    lessons: SnapshotStateList<LessonTime>,
    change: Int,
    lessonDurationString: String,
    breakDurationString: String,
    allBlocksForNumbering: List<SnapshotStateList<LessonTime>>
) {
    val lessonMinutes = lessonDurationString.filter { it.isDigit() }.toIntOrNull() ?: 45
    val breakMinutes = breakDurationString.filter { it.isDigit() }.toIntOrNull() ?: 10

    if (change > 0) {
        var maxLessonNum = 0
        allBlocksForNumbering.forEach { block -> block.forEach { lesson -> if (lesson.lessonNumber > maxLessonNum) maxLessonNum = lesson.lessonNumber } }
        val newLessonNumber = maxLessonNum + 1

        val newStartTimeMinutes: Int
        if (lessons.isEmpty()) {
            val blockBaseTime = when (lessons) {
                allBlocksForNumbering.getOrNull(0) -> "08:00" // Morning
                allBlocksForNumbering.getOrNull(1) -> allBlocksForNumbering.getOrNull(0)?.lastOrNull()?.endTime?.let { minutesToTime(timeToMinutes(it) + breakMinutes) } ?: "13:00"
                allBlocksForNumbering.getOrNull(2) -> allBlocksForNumbering.getOrNull(1)?.lastOrNull()?.endTime?.let { minutesToTime(timeToMinutes(it) + breakMinutes) } ?: "18:00"
                else -> "08:00"
            }
            newStartTimeMinutes = timeToMinutes(blockBaseTime)
        } else {
            newStartTimeMinutes = timeToMinutes(lessons.last().endTime) + breakMinutes
        }
        lessons.add(LessonTime(newLessonNumber, minutesToTime(newStartTimeMinutes), minutesToTime(newStartTimeMinutes + lessonMinutes)))
         // After adding, call updateAllLessonTimes to ensure the new lesson's end time and subsequent lessons are correct
        val addedLesson = lessons.last()
        val blockIndex = allBlocksForNumbering.indexOf(lessons)
        if (blockIndex != -1) {
            updateAllLessonTimes(allBlocksForNumbering, lessonDurationString, breakDurationString, startingFromLesson = addedLesson, startBlockIndex = blockIndex, startLessonIndexInBlock = lessons.size -1)
        } else {
             updateAllLessonTimes(allBlocksForNumbering, lessonDurationString, breakDurationString) // Fallback to global update
        }

    } else if (change < 0 && lessons.isNotEmpty()) {
        lessons.removeLast()
        // After removing, a full recalculation might be safest if inter-lesson dependencies are complex,
        // or intelligently update from the point of removal if possible.
        // For simplicity with current updateAllLessonTimes, a global recalculation is done.
        // However, if the removed lesson was the last in its block and not the last overall, 
        // we might not need to update anything beyond clearing its display.
        // If it affects subsequent blocks, then update is needed.
        // Let's call updateAllLessonTimes without a specific starting point to refresh all dependent times.
        updateAllLessonTimes(allBlocksForNumbering, lessonDurationString, breakDurationString) 
    }
}

@Composable fun SettingCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.35f))) { Column { content() } }
}
@Composable fun SettingItem(title: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = title, color = TextPrimary, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) { Text(text = value, color = TextSecondary, fontSize = 16.sp); Spacer(Modifier.width(4.dp)); Icon(Icons.Default.KeyboardArrowRight, "Navigate", tint = TextSecondary, modifier = Modifier.size(20.dp)) }
    }
}
@Composable fun LessonBlockCard(blockTitle: String, lessons: SnapshotStateList<LessonTime>, onLessonCountChange: (Int) -> Unit, onLessonItemClick: (lesson: LessonTime, blockTitle: String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.35f))) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = blockTitle, color = TextPrimary, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onLessonCountChange(-1) }) { Icon(Icons.Default.Remove, "减少课程", tint = TextPrimary, modifier = Modifier.size(24.dp)) }
                    Text("${lessons.size}节", color = TextPrimary, fontSize = 16.sp)
                    IconButton(onClick = { onLessonCountChange(1) }) { Icon(Icons.Default.Add, "增加课程", tint = TextPrimary, modifier = Modifier.size(24.dp)) }
                }
            }
            lessons.forEachIndexed { index, lesson ->
                if (index > 0) { Divider(color = Color.White.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp)) }
                LessonTimeItem(lesson = lesson, onClick = { onLessonItemClick(lesson, blockTitle) })
            }
        }
    }
}
@Composable fun LessonTimeItem(lesson: LessonTime, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("第${lesson.lessonNumber}节", color = TextPrimary, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) { Text("${lesson.startTime}-${lesson.endTime}", color = TextSecondary, fontSize = 16.sp); Spacer(Modifier.width(4.dp)); Icon(Icons.Default.KeyboardArrowRight, "Navigate", tint = TextSecondary, modifier = Modifier.size(20.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    modifier: Modifier = Modifier, items: List<String>, initialIndex: Int, onItemSelected: (index: Int) -> Unit, label: String? = null
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex.coerceIn(0, items.size -1))
    val coroutineScope = rememberCoroutineScope()
    val itemHeightPx = with(LocalDensity.current) { 40.dp.toPx() }
    LaunchedEffect(key1 = items, key2 = initialIndex) { 
        if(items.isNotEmpty()){
             val targetScrollPosition = (listState.layoutInfo.viewportSize.height / 2 - itemHeightPx / 2).toInt()
             listState.scrollToItem(initialIndex.coerceIn(0, items.size -1), targetScrollPosition)
        }
    }
    Box(modifier = modifier.height(160.dp), contentAlignment = Alignment.Center) { 
        Divider(Modifier.fillMaxWidth().align(Alignment.Center).offset(y = (-20).dp), color = Color.White.copy(alpha = 0.4f), thickness = 1.dp)
        Divider(Modifier.fillMaxWidth().align(Alignment.Center).offset(y = 20.dp), color = Color.White.copy(alpha = 0.4f), thickness = 1.dp)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(vertical = 60.dp)) {
            itemsIndexed(items) { index, item ->
                val isSelected = remember { mutableStateOf(false) }
                LaunchedEffect(listState.firstVisibleItemScrollOffset, listState.firstVisibleItemIndex) {
                    val centerIndex = listState.firstVisibleItemIndex + (listState.layoutInfo.visibleItemsInfo.size / 2)
                    isSelected.value = index == centerIndex && listState.layoutInfo.visibleItemsInfo.isNotEmpty()
                    if (isSelected.value) { onItemSelected(index) }
                }
                val scale by animateFloatAsState(if (isSelected.value) 1.2f else 1.0f, label = "s")
                val itemAlpha by animateFloatAsState(if (isSelected.value) 1f else 0.5f, label = "a")
                val textColor = if (isSelected.value) TextPrimary else TextSecondary
                Box(Modifier.height(40.dp).fillMaxWidth().clickable { 
                    coroutineScope.launch { 
                        listState.animateScrollToItem(index, (listState.layoutInfo.viewportSize.height / 2 - itemHeightPx / 2).toInt()) 
                    }
                }, contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item, color = textColor, fontSize = (20 * scale).sp, fontWeight = if (isSelected.value) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.alpha(itemAlpha), style = androidx.compose.ui.text.TextStyle(platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)))
                        if (isSelected.value && label != null) { Text(label, color = TextPrimary, fontSize = (16 * scale).sp, modifier = Modifier.padding(start = 4.dp).alpha(itemAlpha), style = androidx.compose.ui.text.TextStyle(platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false))) }
                    }
                }
            }
        }
    }
}

@Composable
fun DurationPickerContent(
    title: String, description: String, currentValue: Int, options: List<Int>, onValueSelected: (Int) -> Unit, onDismissRequest: () -> Unit
) {
    var selectedValue by remember { mutableStateOf(currentValue) }
    Card(
        Modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(12.dp), 
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.35f)) 
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismissRequest) { Icon(Icons.Default.Close, "关闭", tint = TextSecondary) }
            }
            Spacer(Modifier.height(8.dp))
            Text(description, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            WheelPicker(items = options.map { it.toString() }, initialIndex = options.indexOf(selectedValue).coerceAtLeast(0), onItemSelected = { index -> selectedValue = options[index] }, label = "分钟")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onValueSelected(selectedValue); onDismissRequest() }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(PurpleBlueAccent), shape = RoundedCornerShape(8.dp)) {
                Text("确定", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonTimePickerContent(
    lessonNumber: Int, 
    blockTitle: String, 
    initialStartTime: String, 
    lessonDurationMinutes: Int, 
    onDismiss: () -> Unit, 
    onConfirm: (newStartHour: Int, newStartMinute: Int) -> Unit
) {
    val initialHour = remember(initialStartTime) { initialStartTime.split(":").getOrNull(0)?.toIntOrNull() ?: 8 }
    val initialMinute = remember(initialStartTime) { initialStartTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0 }
    
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    var hourInput by remember { mutableStateOf(String.format("%02d", initialHour)) }
    var minuteInput by remember { mutableStateOf(String.format("%02d", initialMinute)) }

    Card(
        Modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(12.dp), 
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("修改 第${lessonNumber}节 ($blockTitle)", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭", tint = TextSecondary) }
            }
            Text("课长: ${lessonDurationMinutes}分钟", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
            
            Row(
                Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceEvenly, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = hourInput,
                    onValueChange = { newValue ->
                        val filteredValue = newValue.filter { it.isDigit() }
                        if (filteredValue.length <= 2) {
                            hourInput = filteredValue
                            filteredValue.toIntOrNull()?.let {
                                if (it in 0..23) {
                                    selectedHour = it
                                }
                            }
                        }
                    },
                    label = { Text("时") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleBlueAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = PurpleBlueAccent,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PurpleBlueAccent
                    )
                )
                Text(" : ", fontSize = 24.sp, color = TextPrimary, modifier = Modifier.padding(horizontal = 4.dp))
                OutlinedTextField(
                    value = minuteInput,
                    onValueChange = { newValue ->
                        val filteredValue = newValue.filter { it.isDigit() }
                        if (filteredValue.length <= 2) {
                            minuteInput = filteredValue
                            filteredValue.toIntOrNull()?.let {
                                if (it in 0..59) {
                                    selectedMinute = it
                                }
                            }
                        }
                    },
                    label = { Text("分") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleBlueAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = PurpleBlueAccent,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PurpleBlueAccent
                    )
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { 
                    val finalHour = hourInput.toIntOrNull() ?: selectedHour
                    val finalMinute = minuteInput.toIntOrNull() ?: selectedMinute
                    if (hourInput.toIntOrNull() != null && finalHour in 0..23 && minuteInput.toIntOrNull() != null && finalMinute in 0..59) {
                         onConfirm(finalHour, finalMinute)
                    } else {
                        Log.d("LessonTimePicker", "Invalid time input: H:$hourInput, M:$minuteInput")
                    }
                }, 
                Modifier.fillMaxWidth(), 
                colors = ButtonDefaults.buttonColors(PurpleBlueAccent), 
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("确定", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2C2F4D)
@Composable
fun PreviewCourseTimeSettingsScreen() { 
    ClassSyncTheme { 
        CourseTimeSettingsScreen(onNavigateBack = {}) 
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2C2F4D)
@Composable
fun PreviewDurationPickerContent() {
    ClassSyncTheme {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(GradientBlue, GradientPurple)))) { 
            Box(modifier = Modifier.fillMaxSize().blur(radius = 16.dp)) { 
            }
            Box( 
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.padding(horizontal = 32.dp)) { 
                    DurationPickerContent("时长", "Desc", 45, (20..90 step 5).toList(), {}, {})
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2C2F4D)
@Composable
fun PreviewLessonTimePickerContent() {
    ClassSyncTheme {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(GradientBlue, GradientPurple)))) { 
            Box(modifier = Modifier.fillMaxSize().blur(radius = 16.dp)) { 
            }
            Box( 
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.padding(horizontal = 32.dp)) { 
                    LessonTimePickerContent(1, "上午", "08:00", 45, {}, { _, _ -> })
                }
            }
        }
    }
}
