package com.example.classsync

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.classsync.ui.theme.ClassSyncTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID

//region Color Definitions
val PurpleBlueDark = Color(0xFF2C2F4D)
val PurpleBlueMedium = Color(0xFF4C507C)
val PurpleBlueLight = Color(0xFF8C9EFF)
val PurpleBlueAccent = Color(0xFF6A7BFF)
val TextPrimary = Color.White
val TextSecondary = Color.LightGray
val GridLineColor = TextSecondary.copy(alpha = 0.5f)
val SelectedItemColor = PurpleBlueLight.copy(alpha = 0.2f)
//endregion

val DAYS_OF_WEEK = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
val CLASS_PERIOD_COUNT = 12 // Assuming 12 class periods in a day
val TIME_COLUMN_WIDTH = 56.dp
val HEADER_HEIGHT = 56.dp
val CELL_HEIGHT = 60.dp
val CLASS_START_TIMES = listOf(
    "08:00", "09:30", "10:15", "11:00",
    "13:00", "14:00", "15:00", "16:00",
    "17:00", "18:00", "19:00", "20:00"
)
val SEMESTER_START_DATE = LocalDate.of(2025, 9, 1)

sealed class Screen {
    object AllCourseSchedules : Screen()
    object CourseScheduleSettings : Screen()
    object CourseSchedule : Screen()
}

// Data class for a single course schedule
data class ScheduleData(
    val id: String = UUID.randomUUID().toString(), // Unique ID for each schedule
    val name: String,
    var isCurrent: Boolean = false,
    val term: String // e.g., "2025-2026-1"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClassSyncTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.AllCourseSchedules) }

                Crossfade(targetState = currentScreen, label = "Screen Crossfade") { screen ->
                    when (screen) {
                        Screen.AllCourseSchedules -> AllCourseSchedulesScreen(
                            // Example: Navigate to settings of the first schedule, or pass ID
                            onNavigateToSettings = { scheduleId ->
                                Log.d("Navigation", "Settings for $scheduleId")
                                currentScreen = Screen.CourseScheduleSettings
                            },
                            // Example: Navigate to view the first schedule, or pass ID
                            onNavigateToCourseSchedule = { scheduleId ->
                                Log.d("Navigation", "View schedule $scheduleId")
                                currentScreen = Screen.CourseSchedule
                            },
                            onNavigateBack = { Log.d("Navigation", "Back from AllSchedules (no-op here)") }
                        )
                        Screen.CourseScheduleSettings -> CourseScheduleSettingsScreen(
                            onNavigateBack = { currentScreen = Screen.AllCourseSchedules }
                        )
                        Screen.CourseSchedule -> CourseScheduleScreen(
                            onNavigateBack = { currentScreen = Screen.AllCourseSchedules }
                        )
                    }
                }
            }
        }
    }
}

data class Course(
    val name: String,
    val location: String,
    val teacher: String?,
    val startWeek: Int,
    val endWeek: Int,
    val dayOfWeek: Int,
    val startClass: Int,
    val endClass: Int,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCourseSchedulesScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: (scheduleId: String) -> Unit,
    onNavigateToCourseSchedule: (scheduleId: String) -> Unit,
    onNavigateBack: () -> Unit // Added for consistency, though might be handled by system back
) {
    var isInSelectionMode by remember { mutableStateOf(false) }
    val schedulesList = remember {
        mutableStateListOf(
            ScheduleData(name = "我的主课表", isCurrent = true, term = "2025-2026-1"),
            ScheduleData(name = "备用课表", term = "2025-2026-1"),
            ScheduleData(name = "考试周课表", term = "2025-2026-2")
        )
    }
    val selectedScheduleIds = remember { mutableStateOf(emptySet<String>()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PurpleBlueDark,
        topBar = {
            SmallTopAppBar(
                title = {
                    Text(
                        if (isInSelectionMode) "已选择 ${selectedScheduleIds.value.size} 项" else "全部课程表",
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    if (isInSelectionMode) {
                        IconButton(onClick = {
                            isInSelectionMode = false
                            selectedScheduleIds.value = emptySet()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "取消选择", tint = TextPrimary)
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) { // Or your actual back navigation logic
                            Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
                        }
                    }
                },
                actions = {
                    if (isInSelectionMode) {
                        if (selectedScheduleIds.value.isNotEmpty()) {
                            IconButton(onClick = {
                                // Placeholder for delete logic
                                val idsToRemove = selectedScheduleIds.value
                                schedulesList.removeAll { it.id in idsToRemove }
                                Log.d("AllSchedules", "Deleting: $idsToRemove")
                                isInSelectionMode = false
                                selectedScheduleIds.value = emptySet()
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除选中", tint = TextPrimary)
                            }
                        }
                    } else {
                        IconButton(onClick = {
                            isInSelectionMode = true
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "进入选择模式", tint = TextPrimary) // Replaced Checklist
                        }
                        IconButton(onClick = {
                            // Placeholder for adding a new schedule
                            val newScheduleName = "新建课程表 ${schedulesList.size + 1}"
                            schedulesList.add(ScheduleData(name = newScheduleName, term = "待设置学期"))
                            Log.d("AllSchedules", "Add new schedule clicked")
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "新建课程表", tint = TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = PurpleBlueDark,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(PurpleBlueDark)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp) // Adjusted padding
        ) {
            if (!isInSelectionMode) {
                Text(
                    text = "点击课程表卡片可切换当前并查看课程",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(schedulesList, key = { it.id }) { schedule ->
                    val isSelected = schedule.id in selectedScheduleIds.value
                    ScheduleCardItem(
                        scheduleData = schedule,
                        isSelected = isSelected,
                        isInSelectionMode = isInSelectionMode,
                        onToggleSelection = {
                            selectedScheduleIds.value = if (isSelected) {
                                selectedScheduleIds.value - schedule.id
                            } else {
                                selectedScheduleIds.value + schedule.id
                            }
                        },
                        onCardClick = {
                            if (isInSelectionMode) {
                                selectedScheduleIds.value = if (isSelected) {
                                    selectedScheduleIds.value - schedule.id
                                } else {
                                    selectedScheduleIds.value + schedule.id
                                }
                            } else {
                                // Set this schedule as current (example logic)
                                schedulesList.forEach { it.isCurrent = (it.id == schedule.id) }
                                // schedulesList.find { it.id == schedule.id }?.isCurrent = true
                                // This requires recomposition, ensure schedulesList is observed correctly.
                                // A simple way is to trigger a recomposition by reassigning to a new list or using SnapshotStateList items.
                                val currentList = SnapshotStateList<ScheduleData>().also { it.addAll(schedulesList) }
                                schedulesList.clear()
                                schedulesList.addAll(currentList.map { sch -> if (sch.id == schedule.id) sch.copy(isCurrent = true) else sch.copy(isCurrent = false) })

                                onNavigateToCourseSchedule(schedule.id)
                            }
                        },
                        onSettingsClick = { onNavigateToSettings(schedule.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleCardItem(
    scheduleData: ScheduleData,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onCardClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onCardClick)
            .then(if (isSelected && isInSelectionMode) Modifier.border(2.dp, PurpleBlueAccent, MaterialTheme.shapes.medium) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && isInSelectionMode) SelectedItemColor else PurpleBlueMedium
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (isInSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PurpleBlueAccent,
                            uncheckedColor = TextSecondary,
                            checkmarkColor = TextPrimary
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = scheduleData.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = scheduleData.term,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (scheduleData.isCurrent && !isInSelectionMode) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.CheckCircle, // Using CheckCircle for "current"
                        contentDescription = "当前课表",
                        tint = PurpleBlueLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (!isInSelectionMode) {
                Button(
                    onClick = onSettingsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleBlueAccent)
                ) {
                    Text("设置", color = TextPrimary)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScheduleScreen(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {
    var currentWeek by remember { mutableStateOf(6) }
    var showWeekPicker by remember { mutableStateOf(false) }

    val sampleCourses = remember {
        mutableStateOf(
            listOf(
                Course("示例课程1", "教学楼A101", "张老师", 1, 10, 1, 1, 2, Color(0xFFF06292)),
                Course("示例课程2", "实验楼B203", "李教授", 2, 12, 3, 3, 4, Color(0xFF4DB6AC)),
                Course("示例课程3", "图书馆305", "王博士", 5, 15, 5, 6, 7, Color(0xFF9575CD))
            )
        )
    }

    val weekDates = remember(currentWeek) {
        val firstMondayOfSemester = SEMESTER_START_DATE.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentWeekMonday = firstMondayOfSemester.plusWeeks((currentWeek - 1).toLong())
        List(7) { i -> currentWeekMonday.plusDays(i.toLong()) }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PurpleBlueDark,
        topBar = {
            SmallTopAppBar(
                title = { Text("2025-2026-1 学期课表", color = TextPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = PurpleBlueDark,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Handle add course click */ },
                containerColor = PurpleBlueAccent,
                contentColor = TextPrimary
            ) {
                Icon(Icons.Filled.Add, "添加课程")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(PurpleBlueDark)
        ) {
            // Day Headers Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HEADER_HEIGHT)
                    .background(PurpleBlueDark), // Header row background
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable Box for Week Picker
                Box(
                    modifier = Modifier
                        .width(TIME_COLUMN_WIDTH)
                        .fillMaxHeight() // Ensure the box fills the header height
                        .background(PurpleBlueMedium.copy(alpha=0.7f)) // Slightly different background for the corner
                        .clickable { showWeekPicker = true }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("第", color = TextPrimary, fontSize = 12.sp)
                        Text("$currentWeek", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("周", color = TextPrimary, fontSize = 12.sp)
                    }
                }
                DAYS_OF_WEEK.forEachIndexed { index, day ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(), // Ensure column fills header height
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = day,
                            textAlign = TextAlign.Center,
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = dateFormatter.format(weekDates[index]),
                            textAlign = TextAlign.Center,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Main Schedule Grid
            Row(modifier = Modifier.fillMaxSize()) {
                // Time/Class Period Column
                Column(
                    modifier = Modifier
                        .width(TIME_COLUMN_WIDTH)
                        .fillMaxHeight() // Ensure it fills the available height
                        .background(PurpleBlueDark) // Time column background
                ) {
                    repeat(CLASS_PERIOD_COUNT) { index ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(CELL_HEIGHT),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = CLASS_START_TIMES[index],
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Course Display Area with Grid Lines
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(PurpleBlueDark) // Main grid background
                ) {
                    val columnWidth = maxWidth / DAYS_OF_WEEK.size
                    val rowHeight = CELL_HEIGHT

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw vertical lines
                        repeat(DAYS_OF_WEEK.size - 1) { i ->
                            val x = (i + 1) * columnWidth.toPx()
                            drawLine(
                                color = GridLineColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        // Draw horizontal lines
                        repeat(CLASS_PERIOD_COUNT) { i -> // Draw for all cells including the last one's bottom
                            val y = (i + 1) * rowHeight.toPx()
                            drawLine(
                                color = GridLineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    sampleCourses.value.filter { course ->
                        currentWeek in course.startWeek..course.endWeek
                    }.forEach { course ->
                        val courseHeight = rowHeight * (course.endClass - course.startClass + 1)
                        val courseY = rowHeight * (course.startClass - 1)
                        val courseX = columnWidth * (course.dayOfWeek - 1)

                        Box(
                            modifier = Modifier
                                .offset(x = courseX, y = courseY)
                                .width(columnWidth)
                                .height(courseHeight)
                                .padding(1.dp) // Minimal padding for the border to show
                                .background(course.color, shape = MaterialTheme.shapes.extraSmall)
                                .border(0.5.dp, TextSecondary.copy(alpha = 0.7f), MaterialTheme.shapes.extraSmall)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 2.dp, vertical=4.dp),
                                verticalArrangement = Arrangement.Top, // Align text to top
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = course.name,
                                    color = Color.Black.copy(alpha = 0.8f), // Darker text for readability on colored bg
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                                Text(
                                    text = "@${course.location}",
                                    color = Color.Black.copy(alpha = 0.7f),
                                    fontSize = 8.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                course.teacher?.let { teacher ->
                                    Text(
                                        text = teacher,
                                        color = Color.Black.copy(alpha = 0.6f),
                                        fontSize = 8.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showWeekPicker) {
            WeekPickerDialog(
                currentWeek = currentWeek,
                onWeekSelected = { selectedWeek ->
                    currentWeek = selectedWeek
                    showWeekPicker = false
                },
                onDismissRequest = { showWeekPicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScheduleSettingsScreen(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PurpleBlueDark,
        topBar = {
            SmallTopAppBar(
                title = { Text("课程表设置", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = PurpleBlueDark,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(PurpleBlueDark)
                .padding(vertical = 8.dp) // Add padding for the first and last group
        ) {
            SettingGroup(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingItem(
                    label = "课程表名称",
                    value = "2025-2026-1",
                    onClick = { /* Handle click */ },
                    showArrow = true
                )
                SettingItem(
                    label = "学期开始时间",
                    value = "2025年8月25日周一",
                    onClick = { /* Handle click */ },
                    showArrow = true,
                    arrowIcon = Icons.Filled.ArrowDropDown
                )
                SettingItemWithDescription(
                    label = "当前周数",
                    value = "第6周", // This should ideally be dynamic
                    description = "根据你选择的开学日期推算当前周数",
                    onClick = { /* Handle click */ }
                )
                SettingItem(
                    label = "学期总周数",
                    value = "18周",
                    onClick = { /* Handle click */ },
                    showArrow = true,
                    arrowIcon = Icons.Filled.ArrowDropDown
                )
                var weekendClassEnabled by remember { mutableStateOf(false) }
                SettingToggleItem(
                    label = "周末有课",
                    initialValue = weekendClassEnabled,
                    onToggle = { weekendClassEnabled = it }
                )
                SettingItemWithDescription(
                    label = "课程时间设置",
                    onClick = { /* Handle click */ },
                    showArrow = true,
                    description = "设置课程节数，调整每节课时间"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingGroup(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                var showNonCurrentWeekCourses by remember { mutableStateOf(false) }
                SettingToggleItem(
                    label = "显示非本周课程",
                    initialValue = showNonCurrentWeekCourses,
                    onToggle = { showNonCurrentWeekCourses = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingGroup(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingItem(
                    label = "课程提醒时间",
                    value = "5分钟前",
                    onClick = { /* Handle click */ },
                    showArrow = true
                )
                SettingItem(
                    label = "课程提醒方式",
                    value = "通知提醒",
                    onClick = { /* Handle click */ },
                    showArrow = true
                )
                var showInCalendarEnabled by remember { mutableStateOf(true) }
                SettingToggleItemWithDescription(
                    label = "在日历和组件中显示",
                    description = "课程将以日程形式在日历及组件中显示",
                    initialValue = showInCalendarEnabled,
                    onToggle = { showInCalendarEnabled = it }
                )
            }
        }
    }
}

@Composable
fun SettingGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PurpleBlueMedium),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
fun SettingItem(
    label: String,
    value: String? = null,
    onClick: () -> Unit,
    showArrow: Boolean = false,
    arrowIcon: ImageVector = Icons.Filled.KeyboardArrowRight
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp), // Increased vertical padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextPrimary, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            value?.let {
                Text(text = it, color = TextSecondary, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
            }
            if (showArrow) {
                Icon(arrowIcon, contentDescription = null, tint = TextSecondary)
            }
        }
    }
}

@Composable
fun SettingItemWithDescription(
    label: String,
    value: String? = null,
    description: String? = null,
    onClick: () -> Unit,
    showArrow: Boolean = false, // Added for consistency, though not used in original based on screenshot
    arrowIcon: ImageVector = Icons.Filled.KeyboardArrowRight
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp) // Increased vertical padding
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextPrimary, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                value?.let {
                    Text(text = it, color = TextSecondary, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                }
                if (showArrow) { // Only show arrow if explicitly true
                    Icon(arrowIcon, contentDescription = null, tint = TextSecondary)
                }
            }
        }
        description?.let {
            Text(text = it, color = TextSecondary.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun SettingToggleItem(
    label: String,
    initialValue: Boolean,
    onToggle: (Boolean) -> Unit
) {
    var checked by remember { mutableStateOf(initialValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                checked = !checked
                onToggle(checked)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp), // Adjusted padding for toggle item
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextPrimary, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                onToggle(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = PurpleBlueAccent,
                checkedTrackColor = PurpleBlueLight.copy(alpha = 0.6f),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = PurpleBlueMedium.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
fun SettingToggleItemWithDescription(
    label: String,
    description: String,
    initialValue: Boolean,
    onToggle: (Boolean) -> Unit
) {
    var checked by remember { mutableStateOf(initialValue) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                checked = !checked
                onToggle(checked)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp) // Adjusted padding
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onToggle(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PurpleBlueAccent,
                    checkedTrackColor = PurpleBlueLight.copy(alpha = 0.6f),
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = PurpleBlueMedium.copy(alpha = 0.6f)
                )
            )
        }
        Text(text = description, color = TextSecondary.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}


@Composable
fun WeekPickerDialog(
    currentWeek: Int,
    onWeekSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PurpleBlueMedium),
            shape = MaterialTheme.shapes.large // More rounded corners for dialog
        ) {
            Column(
                modifier = Modifier.padding(24.dp), // Increased padding
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("切换周课表", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(20.dp)) // Increased spacing
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // Add spacing between rows
                    // Assuming max 18-20 weeks, 3 or 4 rows
                    val totalWeeks = 18 // Example, can be dynamic
                    val weeksPerRow = 6
                    val numRows = (totalWeeks + weeksPerRow - 1) / weeksPerRow

                    repeat(numRows) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally) // Spacing and center
                        ) {
                            repeat(weeksPerRow) { column ->
                                val weekNumber = row * weeksPerRow + column + 1
                                if (weekNumber <= totalWeeks) {
                                    Button(
                                        onClick = { onWeekSelected(weekNumber) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp), // Ensure buttons are easy to tap
                                        enabled = weekNumber != currentWeek,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (weekNumber == currentWeek) PurpleBlueAccent else PurpleBlueLight,
                                            disabledContainerColor = PurpleBlueDark.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Text("$weekNumber", color = TextPrimary)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f)) // Maintain layout
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp)) // Increased spacing
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleBlueAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("取消", color = TextPrimary)
                }
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF2C2F4D)
@Composable
fun AllCourseSchedulesScreenPreview() {
    ClassSyncTheme {
        AllCourseSchedulesScreen(
            onNavigateToSettings = {},
            onNavigateToCourseSchedule = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2C2F4D)
@Composable
fun CourseScheduleScreenPreview() {
    ClassSyncTheme {
        CourseScheduleScreen(onNavigateBack = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2C2F4D)
@Composable
fun CourseScheduleSettingsScreenPreview() {
    ClassSyncTheme {
        CourseScheduleSettingsScreen(onNavigateBack = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2C2F4D)
@Composable
fun WeekPickerDialogPreview() {
    ClassSyncTheme {
        WeekPickerDialog(currentWeek = 6, onWeekSelected = {}, onDismissRequest = {})
    }
}

