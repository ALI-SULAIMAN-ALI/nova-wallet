package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.evmErc20

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.TransferExtrinsic
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.EvmAssetHistory
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.EtherscanTransactionsApi
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.EtherscanAccountTransfer
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.feeUsed
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.requireErc20
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlin.time.Duration.Companion.seconds

class EvmErc20AssetHistory(
    private val etherscanTransactionsApi: EtherscanTransactionsApi,
) : EvmAssetHistory() {
    override suspend fun fetchEtherscanOperations(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId,
        apiUrl: String,
        page: Int,
        pageSize: Int,
    ): List<Operation> {
        val erc20Config = chainAsset.requireErc20()
        val accountAddress = chain.addressOf(accountId)

        val response = etherscanTransactionsApi.getErc20Transfers(
            baseUrl = apiUrl,
            contractAddress = erc20Config.contractAddress,
            accountAddress = accountAddress,
            pageNumber = page,
            pageSize = pageSize,
            chainId = chain.id
        )

        return response.result.map { mapRemoteTransferToOperation(it, chainAsset, accountAddress) }
    }

    override suspend fun fetchOperationsForBalanceChange(
        chain: Chain,
        chainAsset: Chain.Asset,
        blockHash: String,
        accountId: AccountId
    ): Result<List<TransferExtrinsic>> {
        // we fetch transfers alongside with balance updates in EvmAssetBalance
        return Result.success(emptyList())
    }

    override fun availableOperationFilters(asset: Chain.Asset): Set<TransactionFilter> {
        return setOf(TransactionFilter.TRANSFER)
    }

    private fun mapRemoteTransferToOperation(
        remote: EtherscanAccountTransfer,
        chainAsset: Chain.Asset,
        accountAddress: String,
    ): Operation {
        return Operation(
            id = remote.hash,
            address = accountAddress,
            type = Operation.Type.Transfer(
                hash = remote.hash,
                myAddress = accountAddress,
                amount = remote.value,
                receiver = remote.to,
                sender = remote.from,
                status = Operation.Status.COMPLETED,
                fee = remote.feeUsed,
            ),
            time = remote.timeStamp.seconds.inWholeMilliseconds,
            chainAsset = chainAsset
        )
    }
}