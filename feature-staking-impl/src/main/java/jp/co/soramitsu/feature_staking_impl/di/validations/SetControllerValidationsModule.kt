package jp.co.soramitsu.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import jp.co.soramitsu.common.di.scope.FeatureScope
import jp.co.soramitsu.common.validation.CompositeValidation
import jp.co.soramitsu.feature_staking_api.domain.api.StakingRepository
import jp.co.soramitsu.feature_staking_impl.domain.validations.controller.IsNotControllerAccountValidation
import jp.co.soramitsu.feature_staking_impl.domain.validations.controller.SetControllerFeeValidation
import jp.co.soramitsu.feature_staking_impl.domain.validations.controller.SetControllerValidationFailure
import jp.co.soramitsu.feature_staking_impl.domain.validations.controller.SetControllerValidationSystem
import jp.co.soramitsu.feature_staking_impl.domain.validations.setup.SetupStakingFeeValidation
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.WalletRepository
import jp.co.soramitsu.feature_wallet_api.domain.validation.EnoughToPayFeesValidation
import jp.co.soramitsu.feature_wallet_api.domain.validation.assetBalanceProducer

@Module
class SetControllerValidationsModule {

    @FeatureScope
    @Provides
    fun provideFeeValidation(): SetControllerFeeValidation {
        return EnoughToPayFeesValidation(
            feeExtractor = { it.fee },
            availableBalanceProducer = { it.transferable },
            errorProducer = { SetControllerValidationFailure.NOT_ENOUGH_TO_PAY_FEES }
        )
    }

    @FeatureScope
    @Provides
    fun provideControllerValidation(
        stakingRepository: StakingRepository
    ) = IsNotControllerAccountValidation(
        stakingRepository = stakingRepository,
        controllerAddressProducer = { it.controllerAddress },
        errorProducer = { SetControllerValidationFailure.ALREADY_CONTROLLER }
    )

    @FeatureScope
    @Provides
    fun provideSetControllerValidationSystem(
        enoughToPayFeesValidation: SetControllerFeeValidation,
        isNotControllerAccountValidation: IsNotControllerAccountValidation
    ) = SetControllerValidationSystem(
        CompositeValidation(
            validations = listOf(
                enoughToPayFeesValidation,
                isNotControllerAccountValidation
            )
        )
    )
}
