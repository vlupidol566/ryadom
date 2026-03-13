package roman.alex.auth

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("phone") val phone: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val phone: String
)

@Serializable
data class RegisterResponse(
    val user: UserDto? = null,
    val error: String? = null
)

@Serializable
data class UsersResponse(
    val users: List<UserDto> = emptyList()
)

@Serializable
data class HelpRequestBody(
    val fromPhone: String,
    val note: String
)

@Serializable
data class HelpRequestResponse(
    val request: HelpRequestPayload? = null,
    val error: String? = null
)

@Serializable
data class HelpRequestPayload(
    val id: String,
    val fromPhone: String,
    val note: String,
    val createdAt: String
)

class UserRepository(
    private val baseUrl: String,
    private val apiToken: String?
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            apiToken?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }
    }

    suspend fun getUsers(search: String?): Result<List<UserDto>> {
        return try {
            val response: HttpResponse = client.get {
                val base = if (search.isNullOrBlank()) {
                    "$baseUrl/users"
                } else {
                    "$baseUrl/users?search=${search.trim()}"
                }
                url(base)
            }
            val parsed: UsersResponse = response.body()
            Result.success(parsed.users)
        } catch (e: Throwable) {
            Log.e("UserRepository", "getUsers error", e)
            Result.failure(e)
        }
    }

    suspend fun sendHelpRequest(fromPhone: String, note: String): Result<HelpRequestPayload> {
        val body = HelpRequestBody(
            fromPhone = fromPhone.trim(),
            note = note.trim()
        )
        return try {
            val response: HttpResponse = client.post {
                url("$baseUrl/help-requests")
                setBody(body)
            }
            val parsed: HelpRequestResponse = response.body()
            when {
                parsed.request != null -> Result.success(parsed.request)
                else -> Result.failure(IllegalStateException(parsed.error ?: "Unknown help request error"))
            }
        } catch (e: Throwable) {
            Log.e("UserRepository", "sendHelpRequest error", e)
            Result.failure(e)
        }
    }

    suspend fun register(name: String, phone: String): Result<UserDto> {
        val body = RegisterRequest(
            name = name.trim(),
            phone = phone.trim()
        )
        return try {
            val response: HttpResponse = client.post {
                url("$baseUrl/users/register")
                setBody(body)
            }
            val parsed: RegisterResponse = response.body()
            when {
                parsed.user != null -> Result.success(parsed.user)
                else -> Result.failure(IllegalStateException(parsed.error ?: "Unknown registration error"))
            }
        } catch (e: Throwable) {
            Log.e("UserRepository", "register error", e)
            Result.failure(e)
        }
    }
}
