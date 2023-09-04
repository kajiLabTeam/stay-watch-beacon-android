package kajilab.togawa.staywatchbeaconandroid.model

data class User (
    val name: String,
    val uuid: String,
    val email: String,
    val communityName: String,
    val latestSyncTime: String
    )