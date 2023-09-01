package kajilab.togawa.staywatchbeaconandroid.viewModel

import kajilab.togawa.staywatchbeaconandroid.model.SignInResult
import kajilab.togawa.staywatchbeaconandroid.state.SignInState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SignInViewModel {
    // firebaseAuth関連
    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    fun onSignInResult(result: SignInResult) {
        _state.update {it.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage
        )}
    }

    fun resetState() {
        _state.update { SignInState() }
    }
}