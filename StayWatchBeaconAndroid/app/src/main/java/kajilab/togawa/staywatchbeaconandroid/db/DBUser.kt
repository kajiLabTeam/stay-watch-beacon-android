package kajilab.togawa.staywatchbeaconandroid.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DBUser(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "uuid") val uuid: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "community_name") val communityName: String?,
    @ColumnInfo(name = "latest_sync_time") val latestSyncTime: String?,
    @ColumnInfo(name = "is_allowed_advertising") val isAllowedAdvertising: Boolean?
)
