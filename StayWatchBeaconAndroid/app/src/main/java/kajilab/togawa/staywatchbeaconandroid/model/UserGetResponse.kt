package kajilab.togawa.staywatchbeaconandroid.model

data class UserGetResponse(
    val id: Int,
    val uuid: String,
    val name: String,
    val role: Int,
    val communityName: String,
)
