package kajilab.togawa.staywatchbeaconandroid.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Query("SELECT * FROM dbuser")
    fun getAllUsers(): List<DBUser>

    @Query("SELECT * FROM dbuser WHERE id LIKE :id")
    fun getUserById(id: Int): DBUser

    @Query("SELECT * FROM dbuser WHERE id IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): List<DBUser>

    @Query("SELECT * FROM dbuser WHERE name LIKE :userName")
    fun getUserByName(userName: String): DBUser

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun createUser(vararg users: DBUser)

    @Delete
    fun deleteUser(user: DBUser)
}