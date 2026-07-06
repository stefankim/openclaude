package com.dockerdroid.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dockerdroid.app.data.db.entities.ComposeProjectEntity
import com.dockerdroid.app.data.db.entities.DeployedContainerEntity
import com.dockerdroid.app.data.db.entities.EventLogEntity
import com.dockerdroid.app.data.db.entities.StackStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ComposeProjectDao {
    @Query("SELECT * FROM compose_projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ComposeProjectEntity>>

    @Query("SELECT * FROM compose_projects ORDER BY updatedAt DESC")
    suspend fun getAllOnce(): List<ComposeProjectEntity>

    @Query("SELECT * FROM compose_projects WHERE id = :id")
    suspend fun byId(id: Long): ComposeProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ComposeProjectEntity): Long

    @Query("UPDATE compose_projects SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun setStatus(id: Long, status: StackStatus, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM compose_projects WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DeployedContainerDao {
    @Query("SELECT * FROM deployed_containers WHERE projectId = :projectId")
    suspend fun forProject(projectId: Long): List<DeployedContainerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<DeployedContainerEntity>)

    @Query("DELETE FROM deployed_containers WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: Long)
}

@Dao
interface EventLogDao {
    @Query("SELECT * FROM event_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<EventLogEntity>>

    @Query("SELECT * FROM event_log WHERE message LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<EventLogEntity>>

    @Insert
    suspend fun insert(entry: EventLogEntity)

    @Query("DELETE FROM event_log WHERE timestamp < :before")
    suspend fun prune(before: Long)
}
