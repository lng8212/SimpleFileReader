package com.longkd.simplefilereader.di

import com.longkd.simplefilereader.data.FileRepositoryImpl
import com.longkd.simplefilereader.domain.FileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModules {
    @Binds
    fun provideFileRepositoryImpl(repository: FileRepositoryImpl): FileRepository
}