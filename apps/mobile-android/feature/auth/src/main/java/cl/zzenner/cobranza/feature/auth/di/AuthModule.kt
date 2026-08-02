package cl.zzenner.cobranza.feature.auth.di

import cl.zzenner.cobranza.core.network.client.TokenProvider
import cl.zzenner.cobranza.feature.auth.data.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    /**
     * Vincula [SessionRepository] (que es @Singleton) como implementación de [TokenProvider].
     * Esto permite que NetworkModule.provideAuthenticatedOkHttpClient inyecte TokenProvider
     * desde SingletonComponent.
     */
    @Binds
    @Singleton
    abstract fun bindTokenProvider(sessionRepository: SessionRepository): TokenProvider
}
