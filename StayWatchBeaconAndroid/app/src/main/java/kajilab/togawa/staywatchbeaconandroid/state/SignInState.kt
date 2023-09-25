package kajilab.togawa.staywatchbeaconandroid.state

data class SignInState (
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null
)