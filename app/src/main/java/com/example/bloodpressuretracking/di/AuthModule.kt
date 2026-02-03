package com.example.bloodpressuretracking.di

import com.example.bloodpressuretracking.data.repository.AmplifyAuthWrapper
import com.example.bloodpressuretracking.data.repository.AmplifyAuthWrapperImpl
import com.example.bloodpressuretracking.data.repository.AuthRepository
import com.example.bloodpressuretracking.data.repository.AuthRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAmplifyAuthWrapper(): AmplifyAuthWrapper {
        return AmplifyAuthWrapperImpl()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(amplifyAuthWrapper: AmplifyAuthWrapper): AuthRepository {
        return AuthRepositoryImpl(amplifyAuthWrapper)
    }
}
