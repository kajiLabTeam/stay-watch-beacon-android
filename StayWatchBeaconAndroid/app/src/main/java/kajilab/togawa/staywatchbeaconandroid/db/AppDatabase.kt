package kajilab.togawa.staywatchbeaconandroid.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DBUser::class], version = 3)  // テーブルのカラムを増やすなど変更がある際versionを一つ上げる必要がある
abstract class AppDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao
}