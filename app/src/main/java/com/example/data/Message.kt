package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_threads")
data class ChatThread(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatThread::class,
            parentColumns = ["id"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("threadId")]
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadId: Long,
    val text: String,
    val isUser: Boolean,
    val thinkingText: String? = null,
    val searchSourcesJson: String? = null, // JSON string representing list of citations
    val appMode: String = "standard", // "standard", "reasoning", "search", "coder"
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM chat_threads ORDER BY createdAt DESC")
    fun getAllThreads(): Flow<List<ChatThread>>

    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp ASC")
    fun getMessagesForThread(threadId: Long): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ChatThread): Long

    @Query("UPDATE chat_threads SET title = :title WHERE id = :id")
    suspend fun updateThreadTitle(id: Long, title: String)

    @Query("DELETE FROM chat_threads WHERE id = :id")
    suspend fun deleteThread(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("DELETE FROM messages WHERE threadId = :threadId")
    suspend fun deleteMessagesForThread(threadId: Long)
}

@Database(entities = [ChatThread::class, Message::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
