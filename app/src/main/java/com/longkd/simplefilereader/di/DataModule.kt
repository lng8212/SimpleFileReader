package com.longkd.simplefilereader.di

import android.content.Context
import com.longkd.simplefilereader.data.FileDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideFileDataSource(@ApplicationContext context: Context): FileDataSource {
        return FileDataSource(context)
    }
}