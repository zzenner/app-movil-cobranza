package cl.zzenner.cobranza.feature.gestion.di

import cl.zzenner.cobranza.feature.gestion.location.AndroidLocationProvider
import cl.zzenner.cobranza.feature.gestion.location.LocationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GestionModule {

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: AndroidLocationProvider): LocationProvider
}
