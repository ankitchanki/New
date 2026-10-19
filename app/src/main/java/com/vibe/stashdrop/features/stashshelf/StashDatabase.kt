package com.vibe.stashdrop.features.stashshelf

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "stash", indices = [Index("createdAt")])
data class StashItemEntity(
    @PrimaryKey val id: String,
    val text: String?,
    val fileName: String?,
    val mimeType: String,
    val collection: String = "Inbox",
    val createdAt: Long = System.currentTimeMillis()
)
@Entity(tableName = "clipboard_history", indices = [Index("createdAt")])
data class ClipboardHistoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val digest: String, val createdAt: Long = System.currentTimeMillis())
@Dao
interface StashDao {
    @Query("SELECT * FROM stash ORDER BY createdAt DESC") fun observe(): Flow<List<StashItemEntity>>
    @Insert suspend fun insert(item: StashItemEntity)
    @Insert suspend fun record(item: ClipboardHistoryEntity)
    @Query("SELECT COUNT(*) FROM clipboard_history WHERE digest=:digest") suspend fun seen(digest: String): Int
    @Query("DELETE FROM clipboard_history WHERE id NOT IN (SELECT id FROM clipboard_history ORDER BY createdAt DESC LIMIT 100)") suspend fun pruneHistory()
    @Query("DELETE FROM stash WHERE id=:id") suspend fun delete(id: String)
    @Query("UPDATE stash SET collection=:collection WHERE id=:id") suspend fun move(id: String, collection: String)
}
@Database(entities = [StashItemEntity::class, ClipboardHistoryEntity::class], version = 1, exportSchema = false)
abstract class StashDatabase : RoomDatabase() {
    abstract fun stash(): StashDao
    companion object {
        fun create(context: Context): StashDatabase = Room.databaseBuilder(context, StashDatabase::class.java, "stashdrop.db").build()
    }
}
