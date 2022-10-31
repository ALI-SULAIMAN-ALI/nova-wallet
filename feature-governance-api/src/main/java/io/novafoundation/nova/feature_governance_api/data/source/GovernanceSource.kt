package io.novafoundation.nova.feature_governance_api.data.source

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.data.repository.OffChainReferendaInfoRepository
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface GovernanceSource {

    val referenda: OnChainReferendaRepository

    val convictionVoting: ConvictionVotingRepository

    val offChainInfo: OffChainReferendaInfoRepository
}

fun ConvictionVotingRepository.trackLocksFlowOrEmpty(voterAccountId: AccountId?, chainId: ChainId): Flow<Map<TrackId, Balance>> {
    return if (voterAccountId != null) {
        trackLocksFlow(voterAccountId, chainId)
    } else {
        flowOf(emptyMap())
    }
}