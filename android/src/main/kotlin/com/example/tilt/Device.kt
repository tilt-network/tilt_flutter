import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.util.*

@Serializable
data class Device(
    var id: String,
    var organizationId: String,
    var apiKeyId: String,
    var platform: String, // Platform
    var version: String,
    var brand: String,
    var model: String,
    var battery: Int,
    var updatedAt: String,
    var createdAt: String,
) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromJson(payload: String): Device? {
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