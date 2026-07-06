package com.dockerdroid.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.dockerdroid.app.data.db.dao.ComposeProjectDao
import com.dockerdroid.app.data.db.dao.DeployedContainerDao
import com.dockerdroid.app.data.db.dao.EventLogDao
import com.dockerdroid.app.data.db.entities.ComposeProjectEntity
import com.dockerdroid.app.data.db.entities.DeployedContainerEntity
import com.dockerdroid.app.data.db.entities.EventLogEntity
import com.dockerdroid.app.data.db.entities.StackStatus

class Converters {
    @TypeConverter fun toStatus(value: String): StackStatus = StackStatus.valueOf(value)
    @TypeConverter fun fromStatus(status: StackStatus): String = status.name
}

@Database(
    entities = [
        ComposeProjectEntity::class,
        DeployedContainerEntity::class,
        EventLogEntity::class,
    ],
    version = 1,
    // Schemas are exported to app/schemas (see build.gradle ksp room.schemaLocation)
    // so version-to-version migrations can be reviewed and tested.
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun composeProjectDao(): ComposeProjectDao
    abstract fun deployedContainerDao(): DeployedContainerDao
    abstract fun eventLogDao(): EventLogDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * Pinned migrations. Empty at v1; add a [Migration] here (never bump the
         * version without one) instead of destroying user data on upgrade.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "dockerdroid.db",
            ).addMigrations(*MIGRATIONS).build().also { instance = it }
        }
    }
}
