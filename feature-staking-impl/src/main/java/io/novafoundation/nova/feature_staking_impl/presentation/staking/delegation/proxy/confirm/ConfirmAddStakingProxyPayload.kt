package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.confirm

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.android.parcel.Parcelize

@Parcelize
class ConfirmAddStakingProxyPayload(
    val fee: FeeParcelModel,
    val proxyAddress: String,
    val newProxyDeposit: Balance,
    val newProxyQuantity: Int
) : Parcelable
