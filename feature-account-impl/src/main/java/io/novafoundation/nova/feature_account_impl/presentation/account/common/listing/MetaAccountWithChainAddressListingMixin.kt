package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.listing.AccountUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class MetaAccountWithChainAddressListingMixinFactory(
    private val walletUiUseCase: WalletUiUseCase,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor
) {

    fun create(
        coroutineScope: CoroutineScope,
        chainId: ChainId,
        selectedAddress: String?
    ): MetaAccountListingMixin {
        return MetaAccountWithChainAddressListingMixin(
            walletUiUseCase = walletUiUseCase,
            resourceManager = resourceManager,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            chainRegistry = chainRegistry,
            chainId = chainId,
            selectedAddress = selectedAddress,
            coroutineScope = coroutineScope
        )
    }
}

private class MetaAccountWithChainAddressListingMixin(
    private val walletUiUseCase: WalletUiUseCase,
    private val resourceManager: ResourceManager,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val chainRegistry: ChainRegistry,
    private val chainId: ChainId,
    private val selectedAddress: String?,
    coroutineScope: CoroutineScope,
) : MetaAccountListingMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val chainFlow by coroutineScope.lazyAsync { chainRegistry.getChain(chainId) }

    override val metaAccountsFlow = metaAccountGroupingInteractor.getControlledMetaAccountsFlow().map { list ->
        list.toListWithHeaders(
            keyMapper = { mapMetaAccountTypeToUi(it, resourceManager) },
            valueMapper = { mapMetaAccountToUi(it) }
        )
    }
        .shareInBackground()

    private suspend fun mapMetaAccountToUi(metaAccount: MetaAccount): AccountUi {
        val icon = walletUiUseCase.walletIcon(metaAccount)

        val chainAddress = metaAccount.addressIn(chainFlow.await())
        val isSelected = chainAddress != null && chainAddress == selectedAddress

        return AccountUi(
            id = metaAccount.id,
            title = metaAccount.name,
            subtitle = mapSubtitle(chainAddress),
            isSelected = isSelected,
            isClickable = chainAddress != null,
            picture = icon,
            subtitleIconRes = if (chainAddress == null) R.drawable.ic_warning_filled else null
        )
    }

    private fun mapSubtitle(address: String?): String {
        return address ?: resourceManager.getString(R.string.account_no_chain_projection)
    }
}