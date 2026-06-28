package kr.yooreka.speedo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kr.yooreka.speedo.data.sensor.lean.AccelTiltLeanProvider
import kr.yooreka.speedo.data.sensor.lean.ComplementaryLeanProvider
import kr.yooreka.speedo.data.sensor.lean.GameRotationVectorLeanProvider
import kr.yooreka.speedo.data.sensor.lean.GravityTiltLeanProvider
import kr.yooreka.speedo.data.sensor.lean.LeanProviderSelector
import kr.yooreka.speedo.data.sensor.lean.RotationVectorLeanProvider
import kr.yooreka.speedo.domain.repository.LeanMeasurement
import kr.yooreka.speedo.domain.repository.LeanProvider
import javax.inject.Singleton

/**
 * lean 측정 전략(F-03)들을 Set 멀티바인딩으로 등록한다. LeanProviderSelector 가 모드별로 매핑해
 * 설정값에 따라 활성 전략을 선택한다. 전략 추가 시 여기에 @IntoSet 바인딩만 추가하면 된다(OCP).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LeanModule {
    @Binds
    @IntoSet
    abstract fun bindGravityTilt(impl: GravityTiltLeanProvider): LeanProvider

    @Binds
    @IntoSet
    abstract fun bindAccelTilt(impl: AccelTiltLeanProvider): LeanProvider

    @Binds
    @IntoSet
    abstract fun bindRotationVector(impl: RotationVectorLeanProvider): LeanProvider

    @Binds
    @IntoSet
    abstract fun bindGameRotationVector(impl: GameRotationVectorLeanProvider): LeanProvider

    @Binds
    @IntoSet
    abstract fun bindComplementary(impl: ComplementaryLeanProvider): LeanProvider

    /** 활성 전략 선택자를 도메인 추상화로 노출(레이어링 유지). */
    @Binds
    @Singleton
    abstract fun bindLeanMeasurement(impl: LeanProviderSelector): LeanMeasurement
}
