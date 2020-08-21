package jp.co.soramitsu.feature_account_impl.presentation

interface AccountRouter {

    fun backToCreateAccountScreen()

    fun backToWelcomeScreen()

    fun openCreatePincode()

    fun openConfirmMnemonicScreen()

    fun backToBackupMnemonicScreen()
}