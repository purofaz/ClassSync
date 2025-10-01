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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.classsync.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID

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
    object CourseTimeSettings : Screen() // 新增：课程时间设置页面
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
                            onNavigateToSettings = { scheduleId ->
                                Log.d("Navigation", "Settings for $scheduleId")
                                currentScreen = Screen.CourseScheduleSettings
                            },
                            onNavigateToCourseSchedule = { scheduleId ->
                                Log.d("Navigation", "View schedule $scheduleId")
                                currentScreen = Screen.CourseSchedule
                            }
                        )
                        Screen.CourseScheduleSettings -> CourseScheduleSettingsScreen(
                            onNavigateBack = { currentScreen = Screen.AllCourseSchedules },
                            onNavigateToCourseTimeSettings = { 
                                Log.d("Navigation", "Navigating to Course Time Settings")
                                currentScreen = Screen.CourseTimeSettings
                            }
                        )
                        Screen.CourseSchedule -> CourseScheduleScreen(
                            onNavigateBack = { currentScreen = Screen.AllCourseSchedules }
                        )
                        Screen.CourseTimeSettings -> CourseTimeSettingsScreen(
                            onNavigateBack = { currentScreen = Screen.CourseScheduleSettings } 
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
    onNavigateToCourseSchedule: (scheduleId: String) -> Unit
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
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(
                colors = listOf(GradientBlue, GradientPurple),
                start = Offset(0f, 0f), 
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY) 
            )),
        containerColor = Color.Transparent, 
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isInSelectionMode) "已选择 ${selectedScheduleIds.value.size} 项" else "全部课程表",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    if (isInSelectionMode) {
                        IconButton(onClick = {
                            isInSelectionMode = false
                            selectedScheduleIds.value = emptySet()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "取消选择", tint = Color.White)
                        }
                    }
                },
                actions = {
                    if (isInSelectionMode) {
                        if (selectedScheduleIds.value.isNotEmpty()) {
                            IconButton(onClick = {
                                val idsToRemove = selectedScheduleIds.value
                                schedulesList.removeAll { it.id in idsToRemove }
                                Log.d("AllSchedules", "Deleting: $idsToRemove")
                                isInSelectionMode = false
                                selectedScheduleIds.value = emptySet()
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除选中", tint = Color.White)
                            }
                        }
                    } else {
                        IconButton(onClick = { isInSelectionMode = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "进入选择模式", tint = Color.White)
                        }
                        IconButton(onClick = {
                            val newScheduleName = "新建课程表 ${schedulesList.size + 1}"
                            schedulesList.add(ScheduleData(name = newScheduleName, term = "待设置学期"))
                            Log.d("AllSchedules", "Add new schedule clicked")
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "新建课程表", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color.Transparent) 
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp) 
        ) {
            if (!isInSelectionMode) {
                Text(
                    text = "点击课程表卡片可切换当前并查看课程\n左滑课程表卡片可以删除",
                    color = Color.White.copy(alpha = 0.7f),
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
                    
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                schedulesList.remove(schedule)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            val color = when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, shape = MaterialTheme.shapes.medium)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        content = {
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
                                        val currentList = SnapshotStateList<ScheduleData>().also { it.addAll(schedulesList) }
                                        schedulesList.clear()
                                        schedulesList.addAll(currentList.map { sch -> if (sch.id == schedule.id) sch.copy(isCurrent = true) else sch.copy(isCurrent = false) })
                                        onNavigateToCourseSchedule(schedule.id)
                                    }
                                },
                                onSettingsClick = { onNavigateToSettings(schedule.id) }
                            )
                        }
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
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            )
            .then(if (isSelected && isInSelectionMode) Modifier.border(2.dp, PurpleBlueAccent, MaterialTheme.shapes.medium) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
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
                            uncheckedColor = Color.White,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = scheduleData.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = scheduleData.term,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                if (scheduleData.isCurrent && !isInSelectionMode) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.CheckCircle, 
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
                    Text("设置", color = Color.White)
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
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(
                colors = listOf(GradientBlue, GradientPurple),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("2025-2026-1 学期课表", color = Color.White, fontSize = 18.sp) }, // White text
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White) // White icon
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Handle add course click */ },
                containerColor = PurpleBlueAccent,
                contentColor = Color.White // White icon on Accent FAB
            ) {
                Icon(Icons.Filled.Add, "添加课程", tint = Color.White) // Explicitly tint icon if needed
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color.Transparent) 
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HEADER_HEIGHT)
                    .background(Color.Transparent),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(TIME_COLUMN_WIDTH)
                        .fillMaxHeight() 
                        .background(Color.White.copy(alpha=0.35f)) 
                        .clickable { showWeekPicker = true }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("第", color = TextPrimary, fontSize = 12.sp) // Black text on light semi-transparent bg
                        Text("$currentWeek", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) // Black text
                        Text("周", color = TextPrimary, fontSize = 12.sp) // Black text
                    }
                }
                DAYS_OF_WEEK.forEachIndexed { index, day ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(), 
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = day,
                            textAlign = TextAlign.Center,
                            color = Color.White, // White text on gradient
                            fontSize = 12.sp
                        )
                        Text(
                            text = dateFormatter.format(weekDates[index]),
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.7f), // Lighter white text on gradient
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(TIME_COLUMN_WIDTH)
                        .fillMaxHeight() 
                        .background(Color.Transparent) 
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
                                color = Color.White.copy(alpha = 0.7f), // Lighter white text on gradient
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(Color.Transparent) 
                ) {
                    val columnWidth = maxWidth / DAYS_OF_WEEK.size
                    val rowHeight = CELL_HEIGHT

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        repeat(DAYS_OF_WEEK.size - 1) { i ->
                            val x = (i + 1) * columnWidth.toPx()
                            drawLine(
                                color = GridLineColor, // Uses new GridLineColor definition
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        repeat(CLASS_PERIOD_COUNT) { i -> 
                            val y = (i + 1) * rowHeight.toPx()
                            drawLine(
                                color = GridLineColor, // Uses new GridLineColor definition
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
                                .padding(1.dp) 
                                .background(course.color, shape = MaterialTheme.shapes.extraSmall)
                                .border(0.5.dp, TextSecondary.copy(alpha = 0.8f), MaterialTheme.shapes.extraSmall) // Darker border
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 2.dp, vertical=4.dp),
                                verticalArrangement = Arrangement.Top, 
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = course.name,
                                    color = Color.Black.copy(alpha = 0.8f), 
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
fun CourseScheduleSettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToCourseTimeSettings: () -> Unit 
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(
                colors = listOf(GradientBlue, GradientPurple),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("课程表设置", color = Color.White) }, // White text
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White) // White icon
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color.Transparent) 
                .padding(vertical = 8.dp) 
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
                    value = "第6周", 
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
                    onClick = onNavigateToCourseTimeSettings, 
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
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.35f)), 
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
    arrowIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp), 
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextPrimary, fontSize = 16.sp) // Black text
        Row(verticalAlignment = Alignment.CenterVertically) {
            value?.let {
                Text(text = it, color = TextSecondary, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp)) // Deep Gray text
            }
            if (showArrow) {
                Icon(arrowIcon, contentDescription = null, tint = TextSecondary) // Deep Gray icon
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
    showArrow: Boolean = false, 
    arrowIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp) 
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextPrimary, fontSize = 16.sp) // Black text
            Row(verticalAlignment = Alignment.CenterVertically) {
                value?.let {
                    Text(text = it, color = TextSecondary, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp)) // Deep Gray text
                }
                if (showArrow) { 
                    Icon(arrowIcon, contentDescription = null, tint = TextSecondary) // Deep Gray icon
                }
            }
        }
        description?.let {
            Text(text = it, color = TextSecondary.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) 
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
            .padding(horizontal = 16.dp, vertical = 8.dp), 
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextPrimary, fontSize = 16.sp) // Black text
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                onToggle(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = PurpleBlueAccent,
                checkedTrackColor = PurpleBlueLight.copy(alpha = 0.6f),
                uncheckedThumbColor = TextSecondary, // Deep Gray thumb
                uncheckedTrackColor = Color.White.copy(alpha = 0.4f) 
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
            .padding(horizontal = 16.dp, vertical = 8.dp) 
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 8.dp)) // Black text
            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onToggle(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PurpleBlueAccent,
                    checkedTrackColor = PurpleBlueLight.copy(alpha = 0.6f),
                    uncheckedThumbColor = TextSecondary, // Deep Gray thumb
                    uncheckedTrackColor = Color.White.copy(alpha = 0.4f) 
                )
            )
        }
        Text(text = description, color = TextSecondary.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
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
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.35f)), 
            shape = MaterialTheme.shapes.large 
        ) {
            Column(
                modifier = Modifier.padding(24.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("切换周课表", style = MaterialTheme.typography.titleMedium, color = TextPrimary) // Black text
                Spacer(Modifier.height(20.dp)) 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { 
                    val totalWeeks = 18 
                    val weeksPerRow = 6
                    val numRows = (totalWeeks + weeksPerRow - 1) / weeksPerRow

                    repeat(numRows) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally) 
                        ) {
                            repeat(weeksPerRow) { column ->
                                val weekNumber = row * weeksPerRow + column + 1
                                if (weekNumber <= totalWeeks) {
                                    Button(
                                        onClick = { onWeekSelected(weekNumber) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp), 
                                        enabled = weekNumber != currentWeek,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (weekNumber == currentWeek) PurpleBlueAccent else Color.White.copy(alpha = 0.3f), 
                                            disabledContainerColor = Color.White.copy(alpha = 0.2f), 
                                            contentColor = Color.White, // White text for all button states
                                            disabledContentColor = Color.White.copy(alpha = 0.7f) // Slightly transparent white for disabled
                                        )
                                    ) {
                                        Text("$weekNumber") // Text color handled by ButtonDefaults
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f)) 
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp)) 
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleBlueAccent,
                        contentColor = Color.White // White text on Accent button
                        ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("取消") // Text color handled by ButtonDefaults
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
            onNavigateToCourseSchedule = {}
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
        CourseScheduleSettingsScreen(onNavigateBack = {}, onNavigateToCourseTimeSettings = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2C2F4D)
@Composable
fun WeekPickerDialogPreview() {
    ClassSyncTheme {
        WeekPickerDialog(currentWeek = 6, onWeekSelected = {}, onDismissRequest = {})
    }
}
