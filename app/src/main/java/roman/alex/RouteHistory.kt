package roman.alex

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_history")
data class RouteHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val destination: String,
    val timestamp: Long = System.currentTimeMillis()
)
