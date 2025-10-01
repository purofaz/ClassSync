package com.example.classsync

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.classsync.data.Course
import com.example.classsync.data.ScheduleData
import com.example.classsync.data.UserPreferencesRepository
import com.example.classsync.ui.theme.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.abs

val DAYS_OF_WEEK = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
const val CLASS_PERIOD_COUNT = 12
val TIME_COLUMN_WIDTH = 56.dp
val HEADER_HEIGHT = 56.dp
val CELL_HEIGHT = 60.dp
val CLASS_START_TIMES = listOf(
    "08:00", "09:30", "10:15", "11:00",
    "13:00", "14:00", "15:00", "16:00",
    "17:00", "18:00", "19:00", "20:00"
)

sealed interface Screen {
    data object AllCourseSchedules : Screen
    data class CourseScheduleSettings(val scheduleId: String?) : Screen
    data class CourseSchedule(val scheduleId: String) : Screen
    data object CourseTimeSettings : Screen
}

class MainActivity : ComponentActivity() {

    private lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferencesRepository = UserPreferencesRepository(applicationContext)
        enableEdgeToEdge()
        setContent {
            ClassSyncTheme {
                val schedulesList = remember { mutableStateListOf<ScheduleData>() }
                var showNonCurrentWeekCourses by remember { mutableStateOf(true) }
                var currentScreen by remember { mutableStateOf<Screen>(Screen.AllCourseSchedules) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    val prefs = userPreferencesRepository.fetchInitialPreferences()
                    schedulesList.clear()

                    // Add sample data if the list is empty for demonstration
                    if (prefs.schedules.isEmpty()) {
                        schedulesList.addAll(createSampleSchedules())
                    } else {
                        schedulesList.addAll(prefs.schedules)
                    }

                    showNonCurrentWeekCourses = prefs.showNonCurrentWeekCourses
                }

                LaunchedEffect(schedulesList) {
                    snapshotFlow { schedulesList.toList() }
                        .debounce(500)
                        .collect { userPreferencesRepository.updateSchedules(it) }
                }

                Crossfade(targetState = currentScreen, label = "Screen Crossfade") { screen ->
                    when (screen) {
                        is Screen.AllCourseSchedules -> AllCourseSchedulesScreen(
                            schedulesList = schedulesList,
                            onNavigateToCreateSchedule = { currentScreen = Screen.CourseScheduleSettings(null) },
                            onNavigateToSettings = { currentScreen = Screen.CourseScheduleSettings(it) },
                            onNavigateToCourseSchedule = { currentScreen = Screen.CourseSchedule(it) }
                        )
                        is Screen.CourseScheduleSettings -> CourseScheduleSettingsScreen(
                            scheduleId = screen.scheduleId,
                            schedulesList = schedulesList,
                            showNonCurrentWeekCourses = showNonCurrentWeekCourses,
                            onShowNonCurrentWeekCoursesChange = { newShow ->
                                showNonCurrentWeekCourses = newShow
                                scope.launch {
                                    userPreferencesRepository.updateShowNonCurrentWeekCourses(newShow)
                                }
                            },
                            onSave = { scheduleId -> currentScreen = Screen.CourseSchedule(scheduleId) },
                            onNavigateBack = { currentScreen = Screen.AllCourseSchedules },
                            onNavigateToCourseTimeSettings = { currentScreen = Screen.CourseTimeSettings }
                        )
                        is Screen.CourseSchedule -> {
                            val schedule = schedulesList.find { schedule: ScheduleData -> schedule.id == screen.scheduleId }
                            if (schedule != null) {
                                CourseScheduleScreen(
                                    schedule = schedule,
                                    showNonCurrentWeekCourses = showNonCurrentWeekCourses,
                                    onNavigateBack = { currentScreen = Screen.AllCourseSchedules }
                                )
                            } else {
                                currentScreen = Screen.AllCourseSchedules
                            }
                        }
                        is Screen.CourseTimeSettings -> CourseTimeSettingsScreen(
                            onNavigateBack = { currentScreen = Screen.CourseScheduleSettings(null) }
                        )
                    }
                }
            }
        }
    }
}

fun calculateCurrentWeek(startDate: LocalDate): Int {
    val today = LocalDate.now()
    if (today.isBefore(startDate)) {
        return 1
    }
    val daysBetween = ChronoUnit.DAYS.between(startDate, today)
    val weekNumber = (daysBetween / 7) + 1
    return weekNumber.toInt().coerceAtLeast(1)
}

fun createSampleSchedules(): List<ScheduleData> {
    val sampleCourses1 = listOf(
        Course(name = "软件工程", location = "教A-101", teacher = "张老师", startWeek = 1, endWeek = 10, dayOfWeek = 1, startClass = 1, endClass = 2, color = Color(0xFFF06292)),
        Course(name = "操作系统", location = "实验B-203", teacher = "李教授", startWeek = 2, endWeek = 12, dayOfWeek = 3, startClass = 3, endClass = 4, color = Color(0xFF4DB6AC)),
        Course(name = "大学物理", location = "图书馆-305", teacher = "王博士", startWeek = 5, endWeek = 15, dayOfWeek = 5, startClass = 6, endClass = 7, color = Color(0xFF9575CD))
    )
    val sampleCourses2 = listOf(
        Course(name = "线性代数", location = "综-C401", teacher = "赵老师", startWeek = 1, endWeek = 16, dayOfWeek = 2, startClass = 1, endClass = 2, color = Color(0xFF4FC3F7)),
        Course(name = "数据结构", location = "电-505", teacher = "孙老师", startWeek = 1, endWeek = 12, dayOfWeek = 4, startClass = 3, endClass = 5, color = Color(0xFFFFD54F))
    )
    return listOf(
        ScheduleData(name = "我的课表", term = "2024-2025-1", isCurrent = true, courses = sampleCourses1),
        ScheduleData(name = "辅修课表", term = "2024-2025-1", courses = sampleCourses2)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCourseSchedulesScreen(
    schedulesList: SnapshotStateList<ScheduleData>,
    modifier: Modifier = Modifier,
    onNavigateToCreateSchedule: () -> Unit,
    onNavigateToSettings: (scheduleId: String) -> Unit,
    onNavigateToCourseSchedule: (scheduleId: String) -> Unit
) {
    var isInSelectionMode by remember { mutableStateOf(false) }
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
                                schedulesList.removeAll { schedule -> schedule.id in idsToRemove }
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
                        IconButton(onClick = onNavigateToCreateSchedule) {
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
                items(schedulesList, key = { it.id }) { schedule: ScheduleData ->
                    val isSelected = schedule.id in selectedScheduleIds.value

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value: SwipeToDismissBoxValue ->
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
                                        val currentId = schedule.id
                                        schedulesList.replaceAll { scheduleItem -> scheduleItem.copy(isCurrent = scheduleItem.id == currentId) }
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
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Checkbox
            if (isInSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.White,
                        uncheckedColor = Color.White.copy(alpha = 0.7f),
                        checkmarkColor = GradientPurple
                    ),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            // Schedule Name and Term
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = scheduleData.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // "Current" Tag
                    if (scheduleData.isCurrent && !isInSelectionMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "当前课表",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = scheduleData.term,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            // Settings Button
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
fun CourseScheduleScreen(
    schedule: ScheduleData,
    showNonCurrentWeekCourses: Boolean,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    val startDate = remember { LocalDate.parse(schedule.semesterStartDate) }
    val initialWeek = remember { calculateCurrentWeek(startDate).coerceIn(1, schedule.totalWeeks) }
    var currentWeek by remember { mutableStateOf(initialWeek) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(
                colors = listOf(GradientBlue, GradientPurple),
            )),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(schedule.name, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            WeekHeader(
                currentWeek = currentWeek,
                totalWeeks = schedule.totalWeeks,
                startDate = startDate,
                onWeekChange = { currentWeek = it }
            )
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cellWidth = (maxWidth - TIME_COLUMN_WIDTH) / DAYS_OF_WEEK.size
                val scrollState = rememberScrollState()

                Box(modifier = Modifier.verticalScroll(scrollState)) {
                    ScheduleGrid(
                        cellHeight = CELL_HEIGHT,
                        cellWidth = cellWidth,
                        timeColumnWidth = TIME_COLUMN_WIDTH
                    )
                    schedule.courses.forEach { course ->
                        if (currentWeek in course.startWeek..course.endWeek) {
                            CourseCard(
                                course = course,
                                cellHeight = CELL_HEIGHT,
                                cellWidth = cellWidth,
                                timeColumnWidth = TIME_COLUMN_WIDTH,
                                isDimmed = !showNonCurrentWeekCourses && currentWeek !in course.startWeek..course.endWeek
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekHeader(
    currentWeek: Int,
    totalWeeks: Int,
    startDate: LocalDate,
    onWeekChange: (Int) -> Unit
) {
    val weekDates = remember(currentWeek) {
        val firstDayOfSemester = startDate.with(DayOfWeek.MONDAY)
        val firstDayOfCurrentWeek = firstDayOfSemester.plusWeeks((currentWeek - 1).toLong())
        List(7) { i -> firstDayOfCurrentWeek.plusDays(i.toLong()) }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (currentWeek > 1) onWeekChange(currentWeek - 1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一周", tint = Color.White)
            }
            Text(
                text = "第 $currentWeek 周",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            IconButton(onClick = { if (currentWeek < totalWeeks) onWeekChange(currentWeek + 1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一周", tint = Color.White)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(TIME_COLUMN_WIDTH))
            DAYS_OF_WEEK.forEachIndexed { index, day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = day, color = Color.White, fontSize = 14.sp)
                    Text(text = dateFormatter.format(weekDates[index]), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ScheduleGrid(cellHeight: androidx.compose.ui.unit.Dp, cellWidth: androidx.compose.ui.unit.Dp, timeColumnWidth: androidx.compose.ui.unit.Dp) {
    Row {
        Column {
            Spacer(modifier = Modifier.height(HEADER_HEIGHT))
            CLASS_START_TIMES.forEachIndexed { index, time ->
                Box(
                    modifier = Modifier.height(cellHeight).width(timeColumnWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}\n$time",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridHeight = CLASS_PERIOD_COUNT * cellHeight.toPx()
            val gridWidth = 7 * cellWidth.toPx()

            // Horizontal lines
            for (i in 0..CLASS_PERIOD_COUNT) {
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, i * cellHeight.toPx() + HEADER_HEIGHT.toPx()),
                    end = Offset(gridWidth, i * cellHeight.toPx() + HEADER_HEIGHT.toPx()),
                    strokeWidth = 1f
                )
            }
            // Vertical lines
            for (i in 0..7) {
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(i * cellWidth.toPx(), HEADER_HEIGHT.toPx()),
                    end = Offset(i * cellWidth.toPx(), gridHeight + HEADER_HEIGHT.toPx()),
                    strokeWidth = 1f
                )
            }
        }
    }
}


@Composable
fun CourseCard(
    course: Course,
    cellHeight: androidx.compose.ui.unit.Dp,
    cellWidth: androidx.compose.ui.unit.Dp,
    timeColumnWidth: androidx.compose.ui.unit.Dp,
    isDimmed: Boolean
) {
    val duration = course.endClass - course.startClass + 1
    val cardHeight = cellHeight * duration
    val offsetX = timeColumnWidth + cellWidth * (course.dayOfWeek - 1)
    val offsetY = HEADER_HEIGHT + cellHeight * (course.startClass - 1)

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(width = cellWidth, height = cardHeight)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(course.color.copy(alpha = if (isDimmed) 0.3f else 0.8f))
            .clickable { /* Handle course click */ }
            .padding(4.dp)
    ) {
        Text(
            text = "${course.name}\n@${course.location}",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis
        )
    }
}
