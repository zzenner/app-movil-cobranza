package cl.zzenner.cobranza.core.network.client

import cl.zzenner.cobranza.core.network.BuildConfig
import cl.zzenner.cobranza.core.network.api.AuthApi
import cl.zzenner.cobranza.core.network.api.GestionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val BASE_URL = BuildConfig.BASE_URL
    private val CONTENT_TYPE = "application/json".toMediaType()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    @Named("public")
    fun providePublicOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            // No registrar cuerpos de login o refresh (pueden contener credenciales)
            if (!message.contains("\"contrasena\"") && !message.contains("\"refreshToken\"")) {
                android.util.Log.d("OkHttp", message)
            }
        }.apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @Named("public")
    fun providePublicRetrofit(
        @Named("public") okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(CONTENT_TYPE))
        .build()

    @Provides
    @Singleton
    fun providePublicAuthApi(@Named("public") retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    @Named("authenticated")
    fun provideAuthenticatedOkHttpClient(
        @Named("public") publicClient: OkHttpClient,
        tokenProvider: TokenProvider,
    ): OkHttpClient {
        val authenticator = SingleFlightAuthenticator(tokenProvider)
        return publicClient.newBuilder()
            .addInterceptor { chain ->
                val token = kotlinx.coroutines.runBlocking { tokenProvider.getAccessToken() }
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    @Named("authenticated")
    fun provideAuthenticatedRetrofit(
        @Named("authenticated") okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(CONTENT_TYPE))
        .build()

    @Provides
    @Singleton
    fun provideSincronizacionApi(
        @Named("authenticated") retrofit: Retrofit,
    ): cl.zzenner.cobranza.core.network.api.SincronizacionApi =
        retrofit.create(cl.zzenner.cobranza.core.network.api.SincronizacionApi::class.java)

    @Provides
    @Singleton
    fun provideGestionApi(@Named("authenticated") retrofit: Retrofit): GestionApi =
        retrofit.create(GestionApi::class.java)

    @Provides
    @Singleton
    fun providePersonaBusquedaApi(
        @Named("authenticated") retrofit: Retrofit,
    ): cl.zzenner.cobranza.core.network.api.PersonaBusquedaApi =
        retrofit.create(cl.zzenner.cobranza.core.network.api.PersonaBusquedaApi::class.java)
}
