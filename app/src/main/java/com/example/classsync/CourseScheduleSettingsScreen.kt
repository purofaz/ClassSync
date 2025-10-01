package com.example.classsync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classsync.data.Course
import com.example.classsync.data.ScheduleData
import com.example.classsync.ui.theme.GradientBlue
import com.example.classsync.ui.theme.GradientPurple
import com.example.classsync.ui.theme.PurpleBlueAccent
import com.example.classsync.ui.theme.TextPrimary
import com.example.classsync.ui.theme.TextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScheduleSettingsScreen(
    scheduleId: String?,
    schedulesList: SnapshotStateList<ScheduleData>,
    showNonCurrentWeekCourses: Boolean,
    onShowNonCurrentWeekCoursesChange: (Boolean) -> Unit,
    onSave: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCourseTimeSettings: () -> Unit
) {
    val isEditing = scheduleId != null
    val scheduleToEdit = remember(scheduleId) { schedulesList.find { it.id == scheduleId } }

    var scheduleName by remember { mutableStateOf(scheduleToEdit?.name ?: "") }
    var termName by remember { mutableStateOf(scheduleToEdit?.term ?: "") }
    var totalWeeks by remember { mutableStateOf(scheduleToEdit?.totalWeeks?.toString() ?: "20") }
    var semesterStartDate by remember { mutableStateOf(scheduleToEdit?.semesterStartDate ?: LocalDate.now().toString()) }
    val courses = remember { mutableStateListOf<Course>().also { it.addAll(scheduleToEdit?.courses ?: emptyList()) } }

    var showDatePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(GradientBlue, GradientPurple))),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑课程表" else "新建课程表", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val newOrUpdatedSchedule = ScheduleData(
                                id = scheduleId ?: UUID.randomUUID().toString(),
                                name = scheduleName.ifBlank { "我的课表" },
                                term = termName.ifBlank { "2024-2025-1" },
                                totalWeeks = totalWeeks.toIntOrNull() ?: 20,
                                semesterStartDate = semesterStartDate,
                                courses = courses,
                                isCurrent = scheduleToEdit?.isCurrent ?: !schedulesList.any { it.isCurrent }
                            )

                            if (isEditing) {
                                val index = schedulesList.indexOfFirst { it.id == scheduleId }
                                if (index != -1) {
                                    schedulesList[index] = newOrUpdatedSchedule
                                }
                            } else {
                                schedulesList.add(newOrUpdatedSchedule)
                            }
                            onSave(newOrUpdatedSchedule.id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleBlueAccent),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("保存", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // General Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = scheduleName,
                        onValueChange = { scheduleName = it },
                        label = { Text("课程表名称") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = getTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = termName,
                        onValueChange = { termName = it },
                        label = { Text("学期名称") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = getTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = totalWeeks,
                            onValueChange = { totalWeeks = it.filter { char -> char.isDigit() } },
                            label = { Text("总周数") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = getTextFieldColors()
                        )
                        OutlinedTextField(
                            value = semesterStartDate,
                            onValueChange = {},
                            label = { Text("开学日期") },
                            readOnly = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker = true },
                            colors = getTextFieldColors()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Courses List Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("课程列表", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        IconButton(onClick = { /* TODO: Add new course logic */ }) {
                            Icon(Icons.Default.Add, contentDescription = "添加课程", tint = TextPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (courses.isEmpty()) {
                        Text(
                            "暂无课程，点击右上角添加",
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false // Inner scrolling is disabled
                        ) {
                            items(courses) { course ->
                                CourseListItem(
                                    course = course,
                                    onEdit = { /* TODO: Edit course logic */ },
                                    onDelete = { courses.remove(course) }
                                )
                            }
                        }
                    }
                }
            }
             Spacer(modifier = Modifier.height(24.dp))

            // Other Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToCourseTimeSettings() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("课程时间设置", color = TextPrimary, fontSize = 16.sp)
                        Icon(Icons.Default.Edit, contentDescription = "编辑时间", tint = TextSecondary)
                    }
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("显示非本周课程", color = TextPrimary, fontSize = 16.sp)
                        Switch(
                            checked = showNonCurrentWeekCourses,
                            onCheckedChange = onShowNonCurrentWeekCoursesChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PurpleBlueAccent,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Instant.parse(semesterStartDate + "T00:00:00Z").toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        semesterStartDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun CourseListItem(course: Course, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(course.name, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "${course.location} | 第${course.startWeek}-${course.endWeek}周 | ${DAYS_OF_WEEK[course.dayOfWeek - 1]} 第${course.startClass}-${course.endClass}节",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "编辑课程", tint = TextSecondary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "删除课程", tint = TextSecondary.copy(alpha = 0.7f))
            }
        }
    }
}


@Composable
private fun getTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PurpleBlueAccent,
    unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
    focusedLabelColor = PurpleBlueAccent,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = PurpleBlueAccent,
    disabledTextColor = TextSecondary,
    disabledLabelColor = TextSecondary,
    disabledBorderColor = Color.White.copy(alpha = 0.4f)
)
