package roman.alex

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteHistoryDao {
    @Query("SELECT * FROM route_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentHistory(): Flow<List<RouteHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: RouteHistory)

    @Query("DELETE FROM route_history")
    suspend fun clearAll()
}
