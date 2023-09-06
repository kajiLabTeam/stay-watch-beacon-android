package kajilab.togawa.staywatchbeaconandroid.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DBUser::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao
}