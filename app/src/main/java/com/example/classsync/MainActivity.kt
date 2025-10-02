package com.example.classsync

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.random.Random

val DAYS_OF_WEEK = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
val TIME_COLUMN_WIDTH = 48.dp
val HEADER_HEIGHT = 0.dp
val CELL_HEIGHT = 72.dp

sealed interface Screen {
    data object AllCourseSchedules : Screen
    data class CourseScheduleSettings(val scheduleId: String?) : Screen
    data class CourseSchedule(val scheduleId: String) : Screen
    data class CourseTimeSettings(val scheduleId: String?) : Screen
}

class MainActivity : ComponentActivity() {

    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferencesRepository = UserPreferencesRepository(applicationContext)
        enableEdgeToEdge()
        setContent {
            ClassSyncTheme {
                val schedulesList = remember { mutableStateListOf<ScheduleData>() }
                var currentScreen by remember { mutableStateOf<Screen>(Screen.AllCourseSchedules) }

                LaunchedEffect(Unit) {
                    val prefs = userPreferencesRepository.fetchInitialPreferences()
                    schedulesList.clear()
                    schedulesList.addAll(prefs.schedules)
                }

                LaunchedEffect(schedulesList) {
                    snapshotFlow { schedulesList.toList() }
                        .debounce(500)
                        .collect { userPreferencesRepository.updateSchedules(it) }
                }

                BackHandler(enabled = currentScreen != Screen.AllCourseSchedules) {
                    when (val screen = currentScreen) {
                        is Screen.CourseScheduleSettings -> {
                            currentScreen = Screen.AllCourseSchedules
                        }
                        is Screen.CourseSchedule -> {
                            currentScreen = Screen.AllCourseSchedules
                        }
                        is Screen.CourseTimeSettings -> {
                            currentScreen = Screen.CourseScheduleSettings(screen.scheduleId)
                        }
                        else -> {
                            // Should not happen as BackHandler is disabled for AllCourseSchedules
                        }
                    }
                }

                AnimatedContent(
                    targetState = currentScreen,
                    label = "Screen Animation",
                    transitionSpec = {
                        val isOpeningCourseSchedule = initialState is Screen.AllCourseSchedules && targetState is Screen.CourseSchedule
                        val isClosingCourseSchedule = initialState is Screen.CourseSchedule && targetState is Screen.AllCourseSchedules

                        if (isOpeningCourseSchedule) {
                            fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.1f, animationSpec = tween(300))
                        } else if (isClosingCourseSchedule) {
                            fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 1.1f, animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.9f, animationSpec = tween(300))
                        } else {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        }
                    }
                ) { screen ->
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
                            onSave = { scheduleId -> currentScreen = Screen.CourseSchedule(scheduleId) },
                            onNavigateBack = { currentScreen = Screen.AllCourseSchedules },
                            onNavigateToCourseTimeSettings = { scheduleId ->
                                if (scheduleId != null) {
                                    currentScreen = Screen.CourseTimeSettings(scheduleId)
                                }
                            }
                        )
                        is Screen.CourseSchedule -> {
                            val schedule = schedulesList.find { it.id == screen.scheduleId }
                            if (schedule != null) {
                                CourseScheduleScreen(
                                    schedule = schedule,
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
                        is Screen.CourseTimeSettings -> {
                            val schedule = schedulesList.find { it.id == screen.scheduleId }
                            if (schedule != null) {
                                CourseTimeSettingsScreen(
                                    classPeriodCount = schedule.classPeriodCount,
                                    classStartTimes = schedule.classStartTimes,
                                    onSettingsChanged = { newCount, newTimes ->
                                        val index = schedulesList.indexOfFirst { it.id == screen.scheduleId }
                                        if (index != -1) {
                                            schedulesList[index] = schedulesList[index].copy(
                                                classPeriodCount = newCount,
                                                classStartTimes = newTimes
                                            )
                                        }
                                    },
                                    onNavigateBack = { currentScreen = Screen.CourseScheduleSettings(screen.scheduleId) }
                                )
                            } else {
                                currentScreen = Screen.AllCourseSchedules
                            }
                        }
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
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var scheduleToDelete by remember { mutableStateOf<ScheduleData?>(null) }
    val context = LocalContext.current
    var showAddMenu by remember { mutableStateOf(false) }


    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("确认删除") },
            text = {
                if (scheduleToDelete != null) {
                    Text("您确定要删除课程表 “${scheduleToDelete!!.name}” 吗？")
                } else {
                    Text("您确定要删除选中的 ${selectedScheduleIds.value.size} 个课程表吗？")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (scheduleToDelete != null) {
                            schedulesList.remove(scheduleToDelete)
                        } else {
                            val idsToRemove = selectedScheduleIds.value
                            schedulesList.removeAll { schedule -> schedule.id in idsToRemove }
                            isInSelectionMode = false
                        }
                        showDeleteConfirmationDialog = false
                        scheduleToDelete = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmationDialog = false
                    scheduleToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }

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
                                showDeleteConfirmationDialog = true
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除选中", tint = Color.White)
                            }
                        }
                    } else {

                        IconButton(onClick = { isInSelectionMode = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "进入选择模式", tint = Color.White)
                        }
                        Box {
                            IconButton(onClick = { showAddMenu = true }) {
                                Icon(Icons.Filled.Add, contentDescription = "添加课程表", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showAddMenu,
                                onDismissRequest = { showAddMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("手动添加课程表") },
                                    onClick = {
                                        onNavigateToCreateSchedule()
                                        showAddMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("教务系统添加课程表") },
                                    onClick = {
                                        Toast.makeText(context, "此功能正在开发中", Toast.LENGTH_SHORT).show()
                                        showAddMenu = false
                                    }
                                )
                            }
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
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                scheduleToDelete = schedule
                                showDeleteConfirmationDialog = true
                                false // Don't dismiss immediately
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
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onScheduleUpdated: (ScheduleData) -> Unit
) {
    val startDate = remember { LocalDate.parse(schedule.semesterStartDate) }
    val initialWeek = remember { calculateCurrentWeek(startDate).coerceIn(1, schedule.totalWeeks) }
    var currentWeek by remember { mutableStateOf(initialWeek) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictingCourses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var courseToSave by remember { mutableStateOf<Course?>(null) }
    var showWeekSelectionDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }

    if (showWeekSelectionDialog) {
        WeekSelectionDialog(
            totalWeeks = schedule.totalWeeks,
            onDismiss = { showWeekSelectionDialog = false },
            onWeekSelected = { week ->
                currentWeek = week
                showWeekSelectionDialog = false
            }
        )
    }

    fun findConflictingCourses(course: Course): List<Course> {
        return schedule.courses.filter { existingCourse ->
            existingCourse.id != course.id &&
                    existingCourse.dayOfWeek == course.dayOfWeek &&
                    existingCourse.weekSet.any { it in course.weekSet } &&
                    (course.startClass <= existingCourse.endClass && course.endClass >= existingCourse.startClass)
        }
    }

    if (showEditDialog) {
        CourseEditDialog(
            course = selectedCourse,
            totalWeeks = schedule.totalWeeks,
            onDismiss = {
                showEditDialog = false
                selectedCourse = null
                selectedCell = null
            },
            onSave = { updatedCourse ->
                val conflicts = findConflictingCourses(updatedCourse)
                if (conflicts.isNotEmpty()) {
                    conflictingCourses = conflicts
                    courseToSave = updatedCourse
                    showConflictDialog = true
                } else {
                    val updatedCourses = schedule.courses.toMutableList()
                    if (selectedCourse?.name?.isBlank() == true) { // New course
                        updatedCourses.add(updatedCourse)
                    } else { // Existing course
                        val index = updatedCourses.indexOfFirst { it.id == updatedCourse.id }
                        if (index != -1) {
                            updatedCourses[index] = updatedCourse
                        }
                    }
                    onScheduleUpdated(schedule.copy(courses = updatedCourses))
                    showEditDialog = false
                    selectedCourse = null
                    selectedCell = null
                }
            },
            onDelete = { courseToDelete ->
                val updatedCourses = schedule.courses.filter { it.id != courseToDelete.id }
                onScheduleUpdated(schedule.copy(courses = updatedCourses))
                showEditDialog = false
                selectedCourse = null
                selectedCell = null
            }
        )
    }

    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = { Text("课程冲突") },
            text = { Text("此时间段与 ${conflictingCourses.joinToString { it.name }} 存在冲突，是否要覆盖？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val coursesToRemove = conflictingCourses.map { it.id }.toSet()
                        val updatedCourses = schedule.courses.filterNot { it.id in coursesToRemove }.toMutableList()
                        courseToSave?.let {
                            val index = updatedCourses.indexOfFirst { c -> c.id == it.id }
                            if (index != -1) {
                                updatedCourses[index] = it
                            } else {
                                updatedCourses.add(it)
                            }
                        }
                        onScheduleUpdated(schedule.copy(courses = updatedCourses))
                        showConflictDialog = false
                        showEditDialog = false
                        selectedCourse = null
                        selectedCell = null
                    }
                ) {
                    Text("覆盖")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConflictDialog = false }) {
                    Text("取消")
                }
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
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .pointerInput(schedule.totalWeeks) {
                    detectHorizontalDragGestures(
                        onDragStart = { },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                dragOffsetX.snapTo(dragOffsetX.value + dragAmount)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                val threshold = 300f
                                if (dragOffsetX.value > threshold) {
                                    currentWeek = (currentWeek - 1).coerceAtLeast(1)
                                } else if (dragOffsetX.value < -threshold) {
                                    currentWeek = (currentWeek + 1).coerceAtMost(schedule.totalWeeks)
                                }
                                dragOffsetX.animateTo(0f, spring())
                            }
                        }
                    )
                }
        ) {
            WeekHeader(
                currentWeek = currentWeek,
                totalWeeks = schedule.totalWeeks,
                startDate = startDate,
                onWeekChange = { currentWeek = it },
                onTitleClick = { showWeekSelectionDialog = true }
            )
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cellWidth = (maxWidth - TIME_COLUMN_WIDTH) / DAYS_OF_WEEK.size
                val scrollState = rememberScrollState()

                Box(modifier = Modifier.verticalScroll(scrollState)) {
                    ScheduleGrid(
                        classPeriodCount = schedule.classPeriodCount,
                        classStartTimes = schedule.classStartTimes,
                        cellHeight = CELL_HEIGHT,
                        cellWidth = cellWidth,
                        timeColumnWidth = TIME_COLUMN_WIDTH,
                        onCellClick = { day, classPeriod ->
                            val clickedCell = Pair(day, classPeriod)
                            if (selectedCell == clickedCell) {
                                selectedCourse = Course(
                                    name = "",
                                    location = "",
                                    teacher = "",
                                    weekSet = setOf(currentWeek),
                                    dayOfWeek = day,
                                    startClass = classPeriod,
                                    endClass = classPeriod,
                                    color = Color(
                                        Random.nextInt(256),
                                        Random.nextInt(256),
                                        Random.nextInt(256)
                                    )
                                )
                                showEditDialog = true
                            } else {
                                selectedCell = clickedCell
                            }
                        }
                    )

                    selectedCell?.let { (day, classPeriod) ->
                        val offsetX = TIME_COLUMN_WIDTH + cellWidth * (day - 1)
                        val offsetY = HEADER_HEIGHT + CELL_HEIGHT * (classPeriod - 1)
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加课程",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .offset(x = offsetX, y = offsetY)
                                .size(width = cellWidth, height = CELL_HEIGHT)
                                .border(
                                    width = 1.5.dp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)

                        )
                    }

                    schedule.courses.forEach { course ->
                        val isCourseInCurrentWeek = currentWeek in course.weekSet
                        if (isCourseInCurrentWeek || schedule.showNonCurrentWeekCourses) {
                            CourseCard(
                                course = course,
                                cellHeight = CELL_HEIGHT,
                                cellWidth = cellWidth,
                                timeColumnWidth = TIME_COLUMN_WIDTH,
                                isDimmed = !isCourseInCurrentWeek,
                                rotationZ = dragOffsetX.value / 50f,
                                onClick = {
                                    selectedCell = null
                                    selectedCourse = course
                                    showEditDialog = true
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
    onWeekChange: (Int) -> Unit,
    onTitleClick: () -> Unit
) {
    val weekDates = remember(currentWeek) {
        val firstDayOfSemester = startDate.with(DayOfWeek.MONDAY)
        val firstDayOfCurrentWeek = firstDayOfSemester.plusWeeks((currentWeek - 1).toLong())
        List(7) { i -> firstDayOfCurrentWeek.plusDays(i.toLong()) }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }
    val monthFormatter = remember(weekDates) {
        val firstMonth = weekDates.first().month.getDisplayName(TextStyle.FULL, Locale.CHINA)
        val lastMonth = weekDates.last().month.getDisplayName(TextStyle.FULL, Locale.CHINA)
        if (firstMonth == lastMonth) firstMonth else "$firstMonth - $lastMonth"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // This Row is removed
        // Row(verticalAlignment = Alignment.CenterVertically) { ... }
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(TIME_COLUMN_WIDTH)
                    .height(40.dp) // Approximate height of the day headers
                    .clickable(onClick = onTitleClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "第 $currentWeek 周",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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
    timeColumnWidth: androidx.compose.ui.unit.Dp,
    onCellClick: (day: Int, classPeriod: Int) -> Unit
) {
    Box {
        Row {
            Column {
                Spacer(modifier = Modifier.height(HEADER_HEIGHT))
                (1..classPeriodCount).forEach { index ->
                    val time = classStartTimes.getOrNull(index - 1) ?: ""
                    Box(
                        modifier = Modifier
                            .height(cellHeight)
                            .width(timeColumnWidth),
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
            Row {
                for (day in 1..7) {
                    Column(modifier = Modifier.width(cellWidth)) {
                        for (classPeriod in 1..classPeriodCount) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(cellHeight)
                                    .clickable { onCellClick(day, classPeriod) }
                            )
                        }
                    }
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridHeight = classPeriodCount * cellHeight.toPx()
            val headerHeightPx = HEADER_HEIGHT.toPx()
            val defaultLineColor = Color.White.copy(alpha = 0.3f)

            // Vertical lines
            for (i in 0..7) {
                val x = i * cellWidth.toPx() + timeColumnWidth.toPx()
                drawLine(
                    color = defaultLineColor,
                    start = Offset(x, headerHeightPx),
                    end = Offset(x, gridHeight + headerHeightPx),
                    strokeWidth = 1.5f
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
    rotationZ: Float,
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
            .graphicsLayer { this.rotationZ = rotationZ }
            .clip(RoundedCornerShape(8.dp))
            .background(course.color.copy(alpha = if (isDimmed) 0.3f else 0.8f))
            .clickable(onClick = onClick)
    ) {
        Text(
            modifier = Modifier
                .padding(4.dp),
            text = "${course.name}\n@${course.location}",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            overflow = TextOverflow.Ellipsis
        )
        if (isDimmed) {
            Surface(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color.Black.copy(alpha = 0.4f)
            ) {
                Text(
                    text = "非本周",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
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
    val isNewCourse = course?.name?.isBlank() == true

    var name by remember { mutableStateOf(course?.name ?: "") }
    var location by remember { mutableStateOf(course?.location ?: "") }
    var teacher by remember { mutableStateOf(course?.teacher ?: "") }
    var selectedWeeks by remember { mutableStateOf(course?.weekSet ?: emptySet()) }
    var dayOfWeek by remember { mutableStateOf(course?.dayOfWeek ?: 1) }
    var startClass by remember { mutableStateOf(course?.startClass?.toString() ?: "1") }
    var endClass by remember { mutableStateOf(course?.endClass?.toString() ?: "1") }

    var nameError by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf(false) }
    var teacherError by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        nameError = name.isBlank()
        locationError = location.isBlank()
        teacherError = teacher.isBlank()
        return !nameError && !locationError && !teacherError
    }

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
                Text(if (isNewCourse) "添加课程" else "编辑课程", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("课程名称") },
                    isError = nameError
                )
                TextField(
                    value = location,
                    onValueChange = { location = it; locationError = false },
                    label = { Text("上课地点") },
                    isError = locationError
                )
                TextField(
                    value = teacher,
                    onValueChange = { teacher = it; teacherError = false },
                    label = { Text("教师") },
                    isError = teacherError
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = startClass,
                        onValueChange = { startClass = it.filter { char -> char.isDigit() } },
                        label = { Text("开始节数") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endClass,
                        onValueChange = { endClass = it.filter { char -> char.isDigit() } },
                        label = { Text("结束节数") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }


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
                    if (!isNewCourse) {
                        TextButton(onClick = { course?.let { onDelete(it) } }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (validate()) {
                            val updatedCourse = (course ?: Course(name = "", location = "", teacher = "", weekSet = emptySet(), dayOfWeek = 1, startClass = 1, endClass = 1, color = Color.White)).copy(
                                name = name,
                                location = location,
                                teacher = teacher,
                                weekSet = selectedWeeks,
                                dayOfWeek = dayOfWeek,
                                startClass = startClass.toIntOrNull() ?: 1,
                                endClass = endClass.toIntOrNull() ?: 1
                            )
                            onSave(updatedCourse)
                        }
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

@Composable
fun WeekSelectionDialog(
    totalWeeks: Int,
    onDismiss: () -> Unit,
    onWeekSelected: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("选择周数", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 50.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(totalWeeks) { week ->
                        val weekNumber = week + 1
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { onWeekSelected(weekNumber) }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = weekNumber.toString(),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
