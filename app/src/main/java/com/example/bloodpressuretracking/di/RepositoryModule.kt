package com.example.bloodpressuretracking.di

import com.example.bloodpressuretracking.data.api.ApiClient
import com.example.bloodpressuretracking.data.repository.AuthRepository
import com.example.bloodpressuretracking.data.repository.BloodPressureRepository
import com.example.bloodpressuretracking.data.repository.BloodPressureRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideBloodPressureRepository(
        apiClient: ApiClient,
        authRepository: AuthRepository
    ): BloodPressureRepository {
        return BloodPressureRepositoryImpl(apiClient, authRepository)
    }
}
