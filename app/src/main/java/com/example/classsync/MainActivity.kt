package com.example.classsync

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.classsync.ui.theme.ClassSyncTheme
import com.example.classsync.ui.theme.GradientBlue
import com.example.classsync.ui.theme.GradientPurple
import com.example.classsync.ui.theme.PurpleBlueAccent
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val DAYS_OF_WEEK = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
val TIME_COLUMN_WIDTH = 56.dp
val HEADER_HEIGHT = 0.dp
val CELL_HEIGHT = 60.dp

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
                var classPeriodCount by remember { mutableStateOf(12) }
                var classStartTimes by remember { mutableStateOf(emptyList<String>()) }
                var currentScreen by remember { mutableStateOf<Screen>(Screen.AllCourseSchedules) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    val prefs = userPreferencesRepository.fetchInitialPreferences()
                    schedulesList.clear()

                    if (prefs.schedules.isEmpty()) {
                        schedulesList.addAll(createSampleSchedules())
                    } else {
                        schedulesList.addAll(prefs.schedules)
                    }

                    showNonCurrentWeekCourses = prefs.showNonCurrentWeekCourses
                    classPeriodCount = prefs.classPeriodCount
                    classStartTimes = prefs.classStartTimes
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
                            val schedule = schedulesList.find { it.id == screen.scheduleId }
                            if (schedule != null) {
                                CourseScheduleScreen(
                                    schedule = schedule,
                                    showNonCurrentWeekCourses = showNonCurrentWeekCourses,
                                    classPeriodCount = classPeriodCount,
                                    classStartTimes = classStartTimes,
                                    onNavigateBack = { currentScreen = Screen.AllCourseSchedules },
                                    onScheduleUpdated = { updatedSchedule ->
                                        val index = schedulesList.indexOfFirst { it.id == updatedSchedule.id }
                                        if (index != -1) {
                                            schedulesList[index] = updatedSchedule
                                        }
                                    }
                                )
                            } else {
                                currentScreen = Screen.AllCourseSchedules
                            }
                        }
                        is Screen.CourseTimeSettings -> CourseTimeSettingsScreen(
                            classPeriodCount = classPeriodCount,
                            classStartTimes = classStartTimes,
                            onSettingsChanged = { newCount, newTimes ->
                                classPeriodCount = newCount
                                classStartTimes = newTimes
                                scope.launch {
                                    userPreferencesRepository.updateClassTimeSettings(newCount, newTimes)
                                }
                            },
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
        Course(name = "软件工程", location = "教A-101", teacher = "张老师", weekSet = (1..10).toSet(), dayOfWeek = 1, startClass = 1, endClass = 2, color = Color(0xFFF06292)),
        Course(name = "操作系统", location = "实验B-203", teacher = "李教授", weekSet = (2..12).toSet(), dayOfWeek = 3, startClass = 3, endClass = 4, color = Color(0xFF4DB6AC)),
        Course(name = "大学物理", location = "图书馆-305", teacher = "王博士", weekSet = (5..15).toSet(), dayOfWeek = 5, startClass = 6, endClass = 7, color = Color(0xFF9575CD))
    )
    val sampleCourses2 = listOf(
        Course(name = "线性代数", location = "综-C401", teacher = "赵老师", weekSet = (1..16).toSet(), dayOfWeek = 2, startClass = 1, endClass = 2, color = Color(0xFF4FC3F7)),
        Course(name = "数据结构", location = "电-505", teacher = "孙老师", weekSet = (1..12).toSet(), dayOfWeek = 4, startClass = 3, endClass = 5, color = Color(0xFFFFD54F))
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

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = scheduleData.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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
    classPeriodCount: Int,
    classStartTimes: List<String>,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onScheduleUpdated: (ScheduleData) -> Unit
) {
    val startDate = remember { LocalDate.parse(schedule.semesterStartDate) }
    val initialWeek = remember { calculateCurrentWeek(startDate).coerceIn(1, schedule.totalWeeks) }
    var currentWeek by remember { mutableStateOf(initialWeek) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }

    if (showDialog) {
        CourseEditDialog(
            course = selectedCourse,
            totalWeeks = schedule.totalWeeks,
            onDismiss = { showDialog = false },
            onSave = { updatedCourse ->
                val updatedCourses = schedule.courses.toMutableList()
                val index = updatedCourses.indexOfFirst { it.id == updatedCourse.id }
                if (index != -1) {
                    updatedCourses[index] = updatedCourse
                }
                onScheduleUpdated(schedule.copy(courses = updatedCourses))
                showDialog = false
            },
            onDelete = { courseToDelete ->
                val updatedCourses = schedule.courses.filter { it.id != courseToDelete.id }
                onScheduleUpdated(schedule.copy(courses = updatedCourses))
                showDialog = false
            }
        )
    }

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
                        classPeriodCount = classPeriodCount,
                        classStartTimes = classStartTimes,
                        cellHeight = CELL_HEIGHT,
                        cellWidth = cellWidth,
                        timeColumnWidth = TIME_COLUMN_WIDTH
                    )
                    schedule.courses.forEach { course ->
                        if (currentWeek in course.weekSet) {
                            CourseCard(
                                course = course,
                                cellHeight = CELL_HEIGHT,
                                cellWidth = cellWidth,
                                timeColumnWidth = TIME_COLUMN_WIDTH,
                                isDimmed = !showNonCurrentWeekCourses,
                                onClick = {
                                    selectedCourse = course
                                    showDialog = true
                                }
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
            .fillMaxWidth(),
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
fun ScheduleGrid(
    classPeriodCount: Int,
    classStartTimes: List<String>,
    cellHeight: androidx.compose.ui.unit.Dp,
    cellWidth: androidx.compose.ui.unit.Dp,
    timeColumnWidth: androidx.compose.ui.unit.Dp
) {
    Row {
        Column {
            Spacer(modifier = Modifier.height(HEADER_HEIGHT))
            (1..classPeriodCount).forEach { index ->
                val time = classStartTimes.getOrNull(index - 1) ?: ""
                Box(
                    modifier = Modifier.height(cellHeight).width(timeColumnWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index\n$time",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridHeight = classPeriodCount * cellHeight.toPx()
            val gridWidth = 7 * cellWidth.toPx()
            val headerHeightPx = HEADER_HEIGHT.toPx()
            val defaultLineColor = Color.White.copy(alpha = 0.3f)
            val separatorLineColor = Color.White.copy(alpha = 0.5f)

            val lunchLineY = 4 * cellHeight.toPx() + headerHeightPx
            drawLine(
                color = separatorLineColor,
                start = Offset(0f, lunchLineY),
                end = Offset(gridWidth, lunchLineY),
                strokeWidth = 1.5f
            )

            val dinnerLineY = 8 * cellHeight.toPx() + headerHeightPx
            drawLine(
                color = separatorLineColor,
                start = Offset(0f, dinnerLineY),
                end = Offset(gridWidth, dinnerLineY),
                strokeWidth = 1.5f
            )

            for (i in 0..7) {
                 drawLine(
                    color = defaultLineColor,
                    start = Offset(i * cellWidth.toPx(), headerHeightPx),
                    end = Offset(i * cellWidth.toPx(), gridHeight + headerHeightPx),
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
    isDimmed: Boolean,
    onClick: () -> Unit
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
            .clickable(onClick = onClick)
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

@Composable
fun CourseEditDialog(
    course: Course?,
    totalWeeks: Int,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
    onDelete: (Course) -> Unit
) {
    if (course == null) return

    var name by remember { mutableStateOf(course.name) }
    var location by remember { mutableStateOf(course.location) }
    var teacher by remember { mutableStateOf(course.teacher ?: "") }
    var selectedWeeks by remember { mutableStateOf(course.weekSet) }
    var dayOfWeek by remember { mutableStateOf(course.dayOfWeek) }
    var startClass by remember { mutableStateOf(course.startClass) }
    var endClass by remember { mutableStateOf(course.endClass) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("编辑课程", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                TextField(value = name, onValueChange = { name = it }, label = { Text("课程名称") })
                TextField(value = location, onValueChange = { location = it }, label = { Text("上课地点") })
                TextField(value = teacher, onValueChange = { teacher = it }, label = { Text("教师") })

                Spacer(modifier = Modifier.height(16.dp))
                Text("选择周数", style = MaterialTheme.typography.titleMedium)
                WeekSelectionGrid(
                    totalWeeks = totalWeeks,
                    selectedWeeks = selectedWeeks,
                    onWeekSelected = { week, isSelected ->
                        selectedWeeks = if (isSelected) {
                            selectedWeeks + week
                        } else {
                            selectedWeeks - week
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onDelete(course) }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val updatedCourse = course.copy(
                            name = name,
                            location = location,
                            teacher = teacher,
                            weekSet = selectedWeeks,
                            dayOfWeek = dayOfWeek,
                            startClass = startClass,
                            endClass = endClass
                        )
                        onSave(updatedCourse)
                    }) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
fun WeekSelectionGrid(
    totalWeeks: Int,
    selectedWeeks: Set<Int>,
    onWeekSelected: (Int, Boolean) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 60.dp),
        modifier = Modifier.height(200.dp)
    ) {
        items(totalWeeks) { week ->
            val weekNumber = week + 1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    onWeekSelected(weekNumber, !selectedWeeks.contains(weekNumber))
                }
            ) {
                Checkbox(
                    checked = selectedWeeks.contains(weekNumber),
                    onCheckedChange = { isChecked ->
                        onWeekSelected(weekNumber, isChecked)
                    }
                )
                Text(text = weekNumber.toString())
            }
        }
    }
}
