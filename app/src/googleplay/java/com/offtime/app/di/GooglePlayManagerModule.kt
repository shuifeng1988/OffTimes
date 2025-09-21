package com.offtime.app.di

import com.offtime.app.manager.GoogleLoginManager
import com.offtime.app.manager.GooglePlayBillingManager
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
abstract class GooglePlayManagerModule {
    
    @Binds
    @Named("google")
    abstract fun bindGoogleLoginManager(googleLoginManager: GoogleLoginManager): LoginManager
    
    @Binds
    abstract fun bindPaymentHelper(helper: com.offtime.app.ui.viewmodel.GooglePlayPaymentHelper): com.offtime.app.ui.viewmodel.PaymentHelper
}

@Module
@InstallIn(SingletonComponent::class)
object GooglePlayManagerProvides {
    
    @Provides
    @Named("google")
    @Singleton
    fun provideGooglePaymentManager(
        context: android.content.Context, 
        userRepository: com.offtime.app.data.repository.UserRepository
    ): PaymentManager {
        return GooglePlayBillingManager(context, userRepository)
    }

    @Provides
    @Named("alipay")
    @Singleton
    fun provideAlipayLoginManager(): LoginManager? = null
    
    @Provides
    @Named("alipay")  
    @Singleton
    fun provideAlipayPaymentManager(): PaymentManager? = null
}