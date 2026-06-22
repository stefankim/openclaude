package com.dockerdroid.app.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Status of an imported Compose stack. */
enum class StackStatus { IMPORTED, DEPLOYING, RUNNING, STOPPED, FAILED }

/** A Compose project imported by the user (the raw YAML is the source of truth). */
@Entity(tableName = "compose_projects")
data class ComposeProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val yaml: String,
    val status: StackStatus = StackStatus.IMPORTED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** A container that belongs to a deployed stack, so we can stop/delete it as a unit. */
@Entity(
    tableName = "deployed_containers",
    foreignKeys = [
        ForeignKey(
            entity = ComposeProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class DeployedContainerEntity(
    @PrimaryKey val containerId: String,
    val projectId: Long,
    val serviceName: String,
    val image: String,
)

/** App-level audit trail surfaced on the Logs screen (separate from container logs). */
@Entity(tableName = "event_log", indices = [Index("timestamp")])
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,
    val tag: String,
    val message: String,
)
