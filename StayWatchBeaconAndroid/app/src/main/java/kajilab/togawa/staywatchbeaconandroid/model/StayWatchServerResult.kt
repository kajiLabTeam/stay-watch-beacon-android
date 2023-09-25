package kajilab.togawa.staywatchbeaconandroid.model

data class StayWatchServerResult (
    val data: StayWatchUser?,
    val errorMessage: String?,
    val errorStatus: Number?
)

data class StayWatchUser(
    val userName: String,
    val uuid: String,
    val communityName: String,
)