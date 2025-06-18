import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@Serializable
data class DeviceData(
    val device: DeviceStatus? = null,
    val task: Task? = null
) {
    val messageType: String?
        get() = when {
            device != null -> "status"
            task != null -> "task"
            else -> null
        }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        private val snakeJson = Json {
            namingStrategy = JsonNamingStrategy.SnakeCase
            ignoreUnknownKeys = true
        }

        fun fromJson(json: String): DeviceData? {
            return try {
                snakeJson.decodeFromString<DeviceData>(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
