package roman.alex

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePlaceDao {
    @Query("SELECT * FROM favorite_places")
    fun getAllFavorites(): Flow<List<FavoritePlace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: FavoritePlace)

    @Query("DELETE FROM favorite_places WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM favorite_places WHERE type = :type LIMIT 1")
    suspend fun getByType(type: FavoriteType): FavoritePlace?
}
