import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.util.*

@Serializable
data class Task(
    val taskId: String,
    val createdAt: String,
    val dataPath: String,
    val dataSize: Int,
    val initiatorId: String,
    val programPath: String,
    val scoreCpu: Int,
    val scoreMem: Int,
) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromJson(payload: String): Task? {
            val json = Json {
                namingStrategy = JsonNamingStrategy.SnakeCase
                ignoreUnknownKeys = true
            }
            return try {
                json.decodeFromString(payload)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}