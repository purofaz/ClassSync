package com.example.classsync

import android.os.Bundle
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

val DAYS_OF_WEEK = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
val CLASS_PERIOD_COUNT = 12 // Assuming 12 class periods in a day
val TIME_COLUMN_WIDTH = 48.dp
val HEADER_HEIGHT = 48.dp
val CELL_HEIGHT = 60.dp // Height for one class period
val CLASS_START_TIMES = listOf(
    "08:00", "09:30", "10:15", "11:00",
    "13:00", "14:00", "15:00", "16:00",
    "17:00", "18:00", "19:00", "20:00"
)
val SEMESTER_START_DATE = LocalDate.of(2025, 9, 1) // Assuming Sept 1, 2025 is a Monday and Week 1

sealed class Screen {
    object AllCourseSchedules : Screen()
    object CourseScheduleSettings : Screen()
    object CourseSchedule : Screen()
}

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
                            onNavigateToSettings = { currentScreen = Screen.CourseScheduleSettings },
                            onNavigateToCourseSchedule = { currentScreen = Screen.CourseSchedule }
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
    val teacher: String?, // 教师信息可能为空
    val startWeek: Int,
    val endWeek: Int,
    val dayOfWeek: Int, // 1 for Monday, 7 for Sunday
    val startClass: Int, // e.g., 1 for the first class
    val endClass: Int,   // e.g., 2 for the second class
    val color: Color     // 课程显示颜色
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCourseSchedulesScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit,
    onNavigateToCourseSchedule: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SmallTopAppBar(
                title = { Text("全部课程表", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back navigation */ }) { /* No-op for now */
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle checkmark click */ }) { /* No-op for now */
                        Icon(Icons.Filled.Check, contentDescription = "选择", tint = Color.White)
                    }
                    IconButton(onClick = { /* Handle add click */ }) { /* No-op for now */
                        Icon(Icons.Filled.Add, contentDescription = "添加", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.Black,
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
                .background(Color.DarkGray) // Set a dark background
                .padding(16.dp)
        ) {
            Text(
                text = "点击课程表卡片可切换当前并查看课程",
                color = Color.LightGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable { onNavigateToCourseSchedule() }, // Handle card click to switch/view schedule
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "2025-2026-1",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "当前",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(Color.Blue.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Button(onClick = onNavigateToSettings) {
                        Text("设置")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScheduleScreen(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {
    var currentWeek by remember { mutableStateOf(6) } // Example: current week is 6
    var showWeekPicker by remember { mutableStateOf(false) }

    val sampleCourses = remember { mutableStateOf(emptyList<Course>())}

    val weekDates = remember(currentWeek) {
        val firstMondayOfSemester = SEMESTER_START_DATE.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentWeekMonday = firstMondayOfSemester.plusWeeks((currentWeek - 1).toLong())
        List(7) { i -> currentWeekMonday.plusDays(i.toLong()) }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { showWeekPicker = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("2025-2026-1 第 ", color = Color.White, fontSize = 16.sp)
                        Text("$currentWeek", color = Color.White, fontSize = 18.sp)
                        Text(" 周", color = Color.White, fontSize = 16.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Handle add course click */ }) {
                Icon(Icons.Filled.Add, "添加课程")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color.DarkGray) // Set a dark background for the schedule
        ) {
            // Day Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HEADER_HEIGHT)
                    .background(Color.Black),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(TIME_COLUMN_WIDTH)) // Corner for time column
                DAYS_OF_WEEK.forEachIndexed { index, day ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = day,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = dateFormatter.format(weekDates[index]),
                            textAlign = TextAlign.Center,
                            color = Color.White,
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
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    repeat(CLASS_PERIOD_COUNT) { index ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(CELL_HEIGHT),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = CLASS_START_TIMES[index], // Class period start time
                                color = Color.White,
                                fontSize = 10.sp // Smaller font for time to fit
                            )
                        }
                    }
                }

                // Course Display Area with Grid Lines
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(Color.Black) // Main grid background
                ) {
                    val columnWidth = maxWidth / DAYS_OF_WEEK.size
                    val rowHeight = CELL_HEIGHT

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw vertical lines
                        repeat(DAYS_OF_WEEK.size - 1) { i ->
                            val x = (i + 1) * columnWidth.toPx()
                            drawLine(
                                color = Color.Gray,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw horizontal lines
                        repeat(CLASS_PERIOD_COUNT - 1) { i ->
                            val y = (i + 1) * rowHeight.toPx()
                            drawLine(
                                color = Color.Gray,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    // Place actual course items here
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
                                .padding(2.dp) // Add some padding around the course item
                                .background(course.color, shape = MaterialTheme.shapes.small)
                                .border(1.dp, Color.LightGray, MaterialTheme.shapes.small)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = course.name,
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = course.location,
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    textAlign = TextAlign.Center
                                )
                                course.teacher?.let { teacher ->
                                    Text(
                                        text = teacher,
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        textAlign = TextAlign.Center
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
        topBar = {
            SmallTopAppBar(
                title = { Text("课程表设置", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.Black,
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
                .background(Color.DarkGray) // Background for the settings screen
        ) {
            // Group 1: Basic Settings
            SettingGroup(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingItem(
                    label = "课程表名称",
                    value = "2025-2026-1",
                    onClick = { /* Handle click */ },
                    showArrow = true
                )
                SettingItem(
                    label = "学期开始时间",
                    value = "2025年8月25日周一", // Assuming this is hardcoded for now
                    onClick = { /* Handle click */ },
                    showArrow = true,
                    arrowIcon = Icons.Filled.ArrowDropDown // Use dropdown arrow
                )
                SettingItemWithDescription(
                    label = "当前周数",
                    value = "第6周",
                    description = "根据你选择的开学日期推算当前周数",
                    onClick = { /* Handle click */ } // The screenshot does not show an arrow, but it's clickable
                )
                SettingItem(
                    label = "学期总周数",
                    value = "18周",
                    onClick = { /* Handle click */ },
                    showArrow = true,
                    arrowIcon = Icons.Filled.ArrowDropDown
                )
                var weekendClassEnabled by remember { mutableStateOf(false) } // State for the toggle
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

            Spacer(modifier = Modifier.height(16.dp)) // Add spacing between groups

            // Group 2: Display Settings
            SettingGroup(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                var showNonCurrentWeekCourses by remember { mutableStateOf(false) } // State for the toggle
                SettingToggleItem(
                    label = "显示非本周课程",
                    initialValue = showNonCurrentWeekCourses,
                    onToggle = { showNonCurrentWeekCourses = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) // Add spacing between groups

            // Group 3: Reminder Settings
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
                var showInCalendarEnabled by remember { mutableStateOf(true) } // State for the toggle
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

// Helper Composable functions for setting items

@Composable
fun SettingGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Gray), // Darker gray for the card background
        shape = MaterialTheme.shapes.medium // Rounded corners for the card
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
    arrowIcon: ImageVector = Icons.Filled.KeyboardArrowRight // Default to right arrow
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            value?.let {
                Text(text = it, color = Color.LightGray, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
            }
            if (showArrow) {
                Icon(arrowIcon, contentDescription = null, tint = Color.LightGray)
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
    arrowIcon: ImageVector = Icons.Filled.KeyboardArrowRight
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                value?.let {
                    Text(text = it, color = Color.LightGray, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                }
                if (showArrow) {
                    Icon(arrowIcon, contentDescription = null, tint = Color.LightGray)
                }
            }
        }
        description?.let {
            Text(text = it, color = Color.LightGray.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
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
            .clickable { checked = !checked; onToggle(checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                onToggle(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Blue, // Adjust colors to match screenshot
                checkedTrackColor = Color.Blue.copy(alpha = 0.6f)
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
            .clickable { checked = !checked; onToggle(checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White, fontSize = 16.sp)
            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onToggle(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Blue, // Adjust colors to match screenshot
                    checkedTrackColor = Color.Blue.copy(alpha = 0.6f)
                )
            )
        }
        Text(text = description, color = Color.LightGray.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun WeekPickerDialog(
    currentWeek: Int,
    onWeekSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("切换周课表", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                Column {
                    repeat(3) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            repeat(6) { column ->
                                val weekNumber = row * 6 + column + 1
                                if (weekNumber <= 18) {
                                    Button(
                                        onClick = { onWeekSelected(weekNumber) },
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .weight(1f),
                                        enabled = weekNumber != currentWeek
                                    ) {
                                        Text(if (weekNumber == 18) "本周" else "$weekNumber")
                                    }
                                } else {
                                    // Empty space for alignment if needed
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismissRequest) {
                    Text("取消")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CourseScheduleScreenPreview() {
    ClassSyncTheme {
        CourseScheduleScreen(onNavigateBack = {}) // Provide an empty lambda for preview
    }
}

@Preview(showBackground = true)
@Composable
fun AllCourseSchedulesScreenPreview() {
    ClassSyncTheme {
        AllCourseSchedulesScreen(onNavigateToSettings = {}, onNavigateToCourseSchedule = {})
    }
}

@Preview(showBackground = true)
@Composable
fun CourseScheduleSettingsScreenPreview() {
    ClassSyncTheme {
        CourseScheduleSettingsScreen(onNavigateBack = {})
    }
}
