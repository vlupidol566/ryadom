package roman.alex

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_places")
data class FavoritePlace(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // "Дом", "Работа", "Любимое кафе"
    val address: String,
    val type: FavoriteType = FavoriteType.OTHER
)

enum class FavoriteType {
    HOME, WORK, OTHER
}
