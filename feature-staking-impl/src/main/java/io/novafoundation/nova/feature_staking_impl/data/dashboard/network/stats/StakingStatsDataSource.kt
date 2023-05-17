package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.common.utils.toPercent
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api.StakingStatsApi
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api.StakingStatsRequest
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api.StakingStatsResponse
import io.novafoundation.nova.runtime.ext.UTILITY_ASSET_ID
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapStakingStringToStakingType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.extensions.requireHexPrefix

interface StakingStatsDataSource {

    suspend fun fetchStakingStats(metaAccount: MetaAccount, stakingChains: List<Chain>): MultiChainStakingStats
}

class RealStakingStatsDataSource(
    private val api: StakingStatsApi
) : StakingStatsDataSource {

    override suspend fun fetchStakingStats(metaAccount: MetaAccount, stakingChains: List<Chain>): MultiChainStakingStats = retryUntilDone {
        val request = StakingStatsRequest(metaAccount, stakingChains)
        val response = api.fetchStakingStats(request).data

        val earnings = response.stakingApies.associatedById()
        val rewards = response.accumulatedRewards.associatedById()
        val activeStakers = response.activeStakers.associatedById()

        val keys = stakingChains.flatMap { chain ->
            chain.utilityAsset.supportedStakingOptions().map { stakingType ->
                StakingOptionId(chain.id, UTILITY_ASSET_ID, stakingType)
            }
        }

        keys.associateWith { key ->
            ChainStakingStats(
                estimatedEarnings = earnings[key]?.maxApy.orZero().asPerbill().toPercent(),
                accountPresentInActiveStakers = key in activeStakers,
                rewards = rewards[key]?.amount.orZero()
            )
        }
    }

    private fun <T : StakingStatsResponse.WithStakingId> SubQueryNodes<T>.associatedById(): Map<StakingOptionId, T> {
        return nodes.associateBy {
            StakingOptionId(
                chainId = it.networkId.requireHexPrefix(),
                chainAssetId = UTILITY_ASSET_ID,
                stakingType = mapStakingStringToStakingType(it.stakingType)
            )
        }
    }
}
