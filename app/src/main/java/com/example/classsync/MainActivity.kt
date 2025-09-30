package com.example.classsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClassSyncTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CourseScheduleScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
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
fun CourseScheduleScreen(modifier: Modifier = Modifier) {
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
                    IconButton(onClick = { /* Handle back navigation */ }) {
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
        CourseScheduleScreen()
    }
}
