package com.example.classsync

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.animation.core.animateFloatAsState
import com.example.classsync.ui.theme.*

// 定义课程时间数据模型
data class LessonTime(
    val lessonNumber: Int,
    var startTime: String,
    var endTime: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTimeSettingsScreen(onNavigateBack: () -> Unit) {
    var lessonDuration by remember { mutableStateOf("45分钟") }
    var breakDuration by remember { mutableStateOf("10分钟") }
    var showLessonDurationDialog by remember { mutableStateOf(false) }
    var showBreakDurationDialog by remember { mutableStateOf(false) }
    var showLessonTimeEditDialog by remember { mutableStateOf(false) }
    var editingLesson by remember { mutableStateOf<LessonTime?>(null) }
    var editingLessonBlockTitle by remember { mutableStateOf<String?>(null) }

    val morningLessons = remember { mutableStateListOf(LessonTime(1, "08:30", "09:15"), LessonTime(2, "09:25", "10:10")) }
    val afternoonLessons = remember { mutableStateListOf(LessonTime(3, "14:00", "14:45"), LessonTime(4, "14:55", "15:40")) }
    val eveningLessons = remember { mutableStateListOf(LessonTime(5, "19:00", "19:45"), LessonTime(6, "19:55", "20:40")) }
    val allBlocks = listOf(morningLessons, afternoonLessons, eveningLessons)

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(GradientBlue, GradientPurple), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))),
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 8.dp)) {
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
        }

        if (showLessonDurationDialog) {
            val currentDurationMinutes = lessonDuration.filter { it.isDigit() }.toIntOrNull() ?: 45
            DurationPickerDialog("每节课上课时长", "将自动调整所有课程的开始及结束时间", currentDurationMinutes, (20..90 step 5).toList(), { selectedMinutes ->
                lessonDuration = "${selectedMinutes}分钟"
                updateAllLessonTimes(allBlocks, lessonDuration, breakDuration, newLessonDuration = "${selectedMinutes}分钟")
                showLessonDurationDialog = false
            }, { showLessonDurationDialog = false })
        }
        if (showBreakDurationDialog) {
            val currentDurationMinutes = breakDuration.filter { it.isDigit() }.toIntOrNull() ?: 10
            DurationPickerDialog("课间休息时长", "将自动调整所有课程的开始及结束时间", currentDurationMinutes, (0..30 step 5).toList(), { selectedMinutes ->
                breakDuration = "${selectedMinutes}分钟"
                updateAllLessonTimes(allBlocks, lessonDuration, breakDuration, newBreakDuration = "${selectedMinutes}分钟")
                showBreakDurationDialog = false
            }, { showBreakDurationDialog = false })
        }
        if (showLessonTimeEditDialog && editingLesson != null && editingLessonBlockTitle != null) {
            val lessonToEdit = editingLesson!!
            val blockTitle = editingLessonBlockTitle!!
            val currentLessonDurationMinutes = lessonDuration.filter { it.isDigit() }.toIntOrNull() ?: 45
            LessonTimePickerDialog(lessonToEdit.lessonNumber, blockTitle, lessonToEdit.startTime, currentLessonDurationMinutes, { showLessonTimeEditDialog = false }) { newStartHour, newStartMinute ->
                val newStartTimeString = String.format("%02d:%02d", newStartHour, newStartMinute)
                lessonToEdit.startTime = newStartTimeString
                var editedBlockIndex = -1; var editedLessonIndexInBlock = -1
                for (blockIdx in allBlocks.indices) {
                    val lessonIdx = allBlocks[blockIdx].indexOf(lessonToEdit)
                    if (lessonIdx != -1) { editedBlockIndex = blockIdx; editedLessonIndexInBlock = lessonIdx; break }
                }
                if (editedBlockIndex != -1) {
                    updateAllLessonTimes(allBlocks, lessonDuration, breakDuration, startingFromLesson = lessonToEdit, startBlockIndex = editedBlockIndex, startLessonIndexInBlock = editedLessonIndexInBlock)
                }
                showLessonTimeEditDialog = false
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
    var processingStartedFromSpecific = false

    if (startingFromLesson != null) {
        val startMinutes = timeToMinutes(startingFromLesson.startTime)
        previousLessonEndTimeTotalMinutes = startMinutes + actualLessonDurationMinutes
        startingFromLesson.endTime = minutesToTime(previousLessonEndTimeTotalMinutes)
        processingStartedFromSpecific = true
    }

    for (currentBlockIdx in allLessons.indices) {
        if (startingFromLesson != null && currentBlockIdx < startBlockIndex) continue
        val lessonBlock = allLessons[currentBlockIdx]
        for (currentLessonIdxInBlock in lessonBlock.indices) {
            if (startingFromLesson != null) {
                if (currentBlockIdx == startBlockIndex && currentLessonIdxInBlock < startLessonIndexInBlock) continue
                if (lessonBlock[currentLessonIdxInBlock] === startingFromLesson && currentBlockIdx == startBlockIndex && currentLessonIdxInBlock == startLessonIndexInBlock) {
                     // This was the starting lesson, its end time is already set, and previousLessonEndTimeTotalMinutes is based on it.
                    // So, for the *next* iteration, previousLessonEndTimeTotalMinutes will be correct.
                    continue // Move to the lesson *after* startingFromLesson
                }
            }

            val lesson = lessonBlock[currentLessonIdxInBlock]
            val currentLessonStartTimeTotalMinutes: Int // Corrected line
            if (previousLessonEndTimeTotalMinutes == -1 && !processingStartedFromSpecific) { // First lesson overall or first lesson of a block if previous was empty
                 // If it's the very first lesson overall and not a specific start, use its own time or default.
                if (allLessons.first().firstOrNull() == lesson ){
                     currentLessonStartTimeTotalMinutes = timeToMinutes(lesson.startTime) // Or a default like 08:00
                } else { // It's likely the start of a new block after an empty one, or something went wrong.
                    // Fallback: use its current start time or a block-specific default if available.
                    currentLessonStartTimeTotalMinutes = timeToMinutes(lesson.startTime) 
                }
            } else {
                currentLessonStartTimeTotalMinutes = previousLessonEndTimeTotalMinutes + actualBreakDurationMinutes
            }
            lesson.startTime = minutesToTime(currentLessonStartTimeTotalMinutes)
            previousLessonEndTimeTotalMinutes = currentLessonStartTimeTotalMinutes + actualLessonDurationMinutes
            lesson.endTime = minutesToTime(previousLessonEndTimeTotalMinutes)
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
    } else if (change < 0 && lessons.isNotEmpty()) {
        lessons.removeLast()
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
    LaunchedEffect(key1 = items, key2 = initialIndex) { // Recenter if items/initialIndex changes
        if(items.isNotEmpty()){
             val targetScrollPosition = (-listState.layoutInfo.viewportSize.height / 2 + itemHeightPx / 2).toInt()
             listState.scrollToItem(initialIndex.coerceIn(0, items.size -1), targetScrollPosition)
        }
    }
    Box(modifier = modifier.height(160.dp), contentAlignment = Alignment.Center) { // Reduced height for picker
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
                Box(Modifier.height(40.dp).fillMaxWidth().clickable { coroutineScope.launch { listState.animateScrollToItem(index, (-listState.layoutInfo.viewportSize.height / 2 + itemHeightPx / 2).toInt()) } }, contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier.offset(y = (-14).dp), // Adjusted text upwards to -14.dp
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item, color = textColor, fontSize = (20 * scale).sp, fontWeight = if (isSelected.value) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.alpha(itemAlpha))
                        if (isSelected.value && label != null) { Text(label, color = TextPrimary, fontSize = (16 * scale).sp, modifier = Modifier.padding(start = 4.dp).alpha(itemAlpha)) }
                    }
                }
            }
        }
    }
}

@Composable
fun DurationPickerDialog(
    title: String, description: String, currentValue: Int, options: List<Int>, onValueSelected: (Int) -> Unit, onDismissRequest: () -> Unit
) {
    var selectedValue by remember { mutableStateOf(currentValue) }
    Dialog(onDismissRequest = onDismissRequest) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Color.White.copy(alpha = 0.35f))) {
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
}

@Composable
fun LessonTimePickerDialog(
    lessonNumber: Int, blockTitle: String, initialStartTime: String, lessonDurationMinutes: Int, onDismiss: () -> Unit, onConfirm: (newStartHour: Int, newStartMinute: Int) -> Unit
) {
    val initialHour = remember(initialStartTime) { initialStartTime.split(":").getOrNull(0)?.toIntOrNull() ?: 8 }
    val initialMinute = remember(initialStartTime) { initialStartTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0 }
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    val hours = (0..23).toList(); val minutes = (0..59).toList()

    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Color.White.copy(alpha = 0.35f))) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("修改 第${lessonNumber}节 ($blockTitle)", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭", tint = TextSecondary) }
                }
                Text("课长: ${lessonDurationMinutes}分钟", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    WheelPicker(Modifier.weight(1f), hours.map { String.format("%02d", it) }, hours.indexOf(selectedHour).coerceAtLeast(0), { selectedHour = hours[it] }, "时")
                    Text(" : ", fontSize = 24.sp, color = TextPrimary, modifier = Modifier.padding(horizontal = 4.dp))
                    WheelPicker(Modifier.weight(1f), minutes.map { String.format("%02d", it) }, minutes.indexOf(selectedMinute).coerceAtLeast(0), { selectedMinute = minutes[it] }, "分")
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = { onConfirm(selectedHour, selectedMinute) }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(PurpleBlueAccent), shape = RoundedCornerShape(8.dp)) {
                    Text("确定", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2C2F4D) @Composable fun PreviewCourseTimeSettingsScreen() { ClassSyncTheme { CourseTimeSettingsScreen(onNavigateBack = {}) } }
@Preview(showBackground = true, backgroundColor = 0xFF2C2F4D) @Composable fun PreviewDurationPickerDialog() { ClassSyncTheme { DurationPickerDialog("时长", "Desc", 45, (0..60 step 5).toList(), {}, {}) } }
@Preview(showBackground = true, backgroundColor = 0xFF2C2F4D) @Composable fun PreviewLessonTimePickerDialog() { ClassSyncTheme { LessonTimePickerDialog(1, "上午", "08:00", 45, {}, {_, _ ->}) } }
