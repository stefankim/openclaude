package com.dockerdroid.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
    // Schema export requires the Room Gradle plugin + a schema dir; disabled until
    // the first stable release wires pinned migrations.
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun composeProjectDao(): ComposeProjectDao
    abstract fun deployedContainerDao(): DeployedContainerDao
    abstract fun eventLogDao(): EventLogDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "dockerdroid.db",
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
