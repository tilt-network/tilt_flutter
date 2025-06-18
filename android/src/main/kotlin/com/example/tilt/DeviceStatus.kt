import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.util.*

@Serializable
data class DeviceStatus(
    var deviceId: String,
    var deviceToken: String,
    var model: String,
    var brand: String,
    var year: Int,
    var batteryLevel: Int,
    var batteryState: String,
    var publicKey: String,
    var isInBatterySaveMode: Boolean,
    var available: Boolean,
    var scoreCpu: Int,
    var scoreMem: Int,
    var createdAt: String = Date().toInstant().toString()
) {
    @OptIn(ExperimentalSerializationApi::class)
    fun toStatusString(): String {
        val json = Json {
            namingStrategy = JsonNamingStrategy.SnakeCase
        }
        return json.encodeToString(this)
        // return json.encodeToString(mapOf("status" to this))
    }
}
