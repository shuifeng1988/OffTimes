package com.offtime.app.di

import com.offtime.app.manager.AlipayLoginManager
import com.offtime.app.manager.AlipayPaymentManager
import com.offtime.app.manager.interfaces.LoginManager
import com.offtime.app.manager.interfaces.PaymentManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlipayManagerModule {
    
    @Binds
    @Named("alipay")
    abstract fun bindAlipayLoginManager(alipayLoginManager: AlipayLoginManager): LoginManager
    
    @Binds
    @Named("alipay")
    abstract fun bindAlipayPaymentManager(alipayPaymentManager: AlipayPaymentManager): PaymentManager
}

@Module
@InstallIn(SingletonComponent::class)
object AlipayManagerProvides {
    
    @Provides
    @Named("google")
    @Singleton
    fun provideGoogleLoginManager(): LoginManager? = null
    
    @Provides
    @Named("google")
    @Singleton
    fun provideGooglePaymentManager(): PaymentManager? = null
}