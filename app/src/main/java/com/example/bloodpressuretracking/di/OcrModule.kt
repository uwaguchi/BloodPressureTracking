package com.example.bloodpressuretracking.di

import com.example.bloodpressuretracking.data.ocr.OcrRepository
import com.example.bloodpressuretracking.data.ocr.OcrRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OcrModule {
    @Binds
    @Singleton
    abstract fun bindOcrRepository(impl: OcrRepositoryImpl): OcrRepository
}
