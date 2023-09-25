package kajilab.togawa.staywatchbeaconandroid.state

data class UserState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null
)
