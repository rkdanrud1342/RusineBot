package supa.duap.modules

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import supa.duap.api.RusineBotAPICallAdapterFactory
import supa.duap.api.RusineBotGsonConverterFactory
import java.util.concurrent.TimeUnit


val networkModule = module {
    single {
        OkHttpClient()
            .newBuilder()
            .apply {
                connectTimeout(timeout = 60L, unit = TimeUnit.SECONDS)
                writeTimeout(timeout = 60L, unit = TimeUnit.SECONDS)
                readTimeout(timeout = 60L, unit = TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        HttpLoggingInterceptor.Level.BODY
                    }
                )
                addInterceptor {
                    Exception(it.request().url.toString()).printStackTrace()
                    it.proceed(it.request())
                }
            }
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl("http://localhost:15382/")
            .addConverterFactory(RusineBotGsonConverterFactory.create())
            .addCallAdapterFactory(RusineBotAPICallAdapterFactory())
            .client(get())
            .build()
    }
}
