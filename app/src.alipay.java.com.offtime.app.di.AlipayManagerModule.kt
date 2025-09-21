import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AlipayManagerModule {

    @Binds
    @Named("alipay")
    abstract fun bindAlipayLoginManager(alipayLoginManager: com.offtime.app.manager.AlipayLoginManager): LoginManager

    @Binds
    @Named("alipay")
    abstract fun bindAlipayPaymentManager(alipayPaymentManager: com.offtime.app.manager.AlipayPaymentManager): PaymentManager
    
    @Binds
    abstract fun bindPaymentHelper(helper: com.offtime.app.ui.viewmodel.AlipayPaymentHelper): com.offtime.app.ui.viewmodel.PaymentHelper
}
