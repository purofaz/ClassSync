package com.example.classsync.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.UUID

@Serializable
data class Course(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val location: String,
    val teacher: String?,
    val startWeek: Int,
    val endWeek: Int,
    val dayOfWeek: Int, // 1 for Monday, 7 for Sunday
    val startClass: Int,
    val endClass: Int,
    @Serializable(with = ColorSerializer::class)
    val color: Color
)

@Serializable
data class ScheduleData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var isCurrent: Boolean = false,
    val term: String,
    val semesterStartDate: String = LocalDate.now().toString(), // ISO-8601 format
    val totalWeeks: Int = 18,
    val courses: List<Course> = emptyList()
)

@Serializable
data class UserPreferences(
    val schedules: List<ScheduleData> = emptyList(),
    val showNonCurrentWeekCourses: Boolean = true
)

object UserPreferencesSerializer : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences = UserPreferences()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        try {
            return Json.decodeFromString(
                UserPreferences.serializer(), input.readBytes().decodeToString()
            )
        } catch (exception: SerializationException) {
            throw CorruptionException("Cannot read json.", exception)
        }
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) =
        output.write(Json.encodeToString(UserPreferences.serializer(), t).encodeToByteArray())
}


object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeInt(value.toArgb())
    }
    override fun deserialize(decoder: Decoder): Color {
        return Color(decoder.decodeInt())
    }
}
