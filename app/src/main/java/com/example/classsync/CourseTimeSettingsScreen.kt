package com.example.classsync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.classsync.ui.theme.GradientBlue
import com.example.classsync.ui.theme.GradientPurple
import com.example.classsync.ui.theme.PurpleBlueAccent
import com.example.classsync.ui.theme.TextPrimary
import com.example.classsync.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTimeSettingsScreen(
    classPeriodCount: Int,
    classStartTimes: List<String>,
    onSettingsChanged: (Int, List<String>) -> Unit,
    onNavigateBack: () -> Unit
) {
    var periodCount by remember { mutableStateOf(classPeriodCount.toString()) }
    val startTimes = remember { mutableStateListOf<String>().also { it.addAll(classStartTimes) } }

    // This effect synchronizes the list when the initial values change or when the count changes
    LaunchedEffect(periodCount) {
        val newCount = periodCount.toIntOrNull() ?: 0
        if (newCount > startTimes.size) {
            // Add new default times
            val lastTime = startTimes.lastOrNull() ?: "08:00"
            var lastHour = lastTime.substringBefore(":").toIntOrNull() ?: 8
            var lastMinute = lastTime.substringAfter(":").toIntOrNull() ?: 0

            repeat(newCount - startTimes.size) {
                lastMinute += 45 // Add lesson duration
                if (lastMinute >= 60) {
                    lastHour += lastMinute / 60
                    lastMinute %= 60
                }
                lastHour %= 24
                startTimes.add(String.format("%02d:%02d", lastHour, lastMinute))
            }
        } else if (newCount < startTimes.size) {
            // Remove excess times
            startTimes.removeRange(newCount, startTimes.size)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(GradientBlue, GradientPurple))),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("课程时间设置", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            onSettingsChanged(periodCount.toIntOrNull() ?: 12, startTimes.toList())
                            onNavigateBack()
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleBlueAccent),
                    ) {
                        Text("保存")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("总课程节数", color = TextPrimary, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = periodCount,
                        onValueChange = { value ->
                            // Allow only digits and ensure it's not excessively large
                            if (value.all { it.isDigit() } && (value.toIntOrNull() ?: 0) <= 20) {
                                periodCount = value
                            }
                        },
                        label = { Text("例如: 12") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = getTextFieldColors()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("各节课开始时间", color = TextPrimary, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Using Column as the content might not be very long and we are already in a scrollable Column
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    startTimes.forEachIndexed { index, time ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "第 ${index + 1} 节",
                                modifier = Modifier.width(80.dp),
                                color = TextPrimary
                            )
                            OutlinedTextField(
                                value = time,
                                onValueChange = { newTime ->
                                    startTimes[index] = newTime
                                },
                                label = { Text("HH:mm") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = getTextFieldColors()
                            )
                        }
                    }
                }
            }
        }
    }
}
