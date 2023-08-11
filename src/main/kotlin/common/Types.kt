package common

import com.squareup.kotlinpoet.ClassName

object Types {
    val DAGGER_MODULE_CLASS = ClassName("dagger", "Module")
    val DAGGER_INSTALLIN = ClassName("dagger.hilt", "InstallIn")
    val DAGGER_PROVIDES = ClassName("dagger", "Provides")
    val DAGGER_SINGLETONCOMPONENT = ClassName("dagger.hilt.components", "SingletonComponent")
    val JAVAX_QUALIFIER = ClassName("javax.inject", "Qualifier")
    val JAVAX_INJECT = ClassName("javax.inject", "Inject")
    val JAVAX_SINGLETON = ClassName("javax.inject", "Singleton")
    val OKHHTP_HTTPLOGGINGINTERCEPTOR = ClassName("okhttp3.logging", "HttpLoggingInterceptor")
    val ANDROID_CONTEXT = ClassName("android.content", "Context")
    val DAGGER_APPLICATIONCONTEXT = ClassName("dagger.hilt.android.qualifiers", "ApplicationContext")
    val CHUCKER_INTERCEPTOR = ClassName("com.chuckerteam.chucker.api", "ChuckerInterceptor")
    val OKHTTP_INTERCEPTOR = ClassName("okhttp3", "Interceptor")
    val OKHTTP_CLIENT = ClassName("okhttp3", "OkHttpClient")
    val FLIPPER_CLIENT = ClassName("com.facebook.flipper.android", "AndroidFlipperClient")
    val FLIPPER_OKHTTPINTERCEPTOR = ClassName("com.facebook.flipper.plugins.network", "FlipperOkhttpInterceptor")
    val FLIPPER_PLUGIN_NETWORK = ClassName("com.facebook.flipper.plugins.network", "NetworkFlipperPlugin")
    val MOSHI = ClassName("com.squareup.moshi", "Moshi")
    val MOSHI_CONVERTER_FACTORY = ClassName("retrofit2.converter.moshi", "MoshiConverterFactory")
    val RETROFIT = ClassName("retrofit2", "Retrofit")
    val COROUTINE_DISPATCHER = ClassName("kotlinx.coroutines", "CoroutineDispatcher")
    val COROUTINE_SCOPE = ClassName("kotlinx.coroutines", "CoroutineScope")
    val COROUTINE_SUPERVISORJOB = ClassName("kotlinx.coroutines", "SupervisorJob")
    val COROUTINE_DISPATCHERS = ClassName("kotlinx.coroutines", "Dispatchers")
    val FLOW = ClassName("kotlinx.coroutines.flow", "Flow")

    object Room {
        val DATABASE = ClassName("androidx.room", "Database")
        val ROOM_DATABASE= ClassName("androidx.room", "RoomDatabase")
        val TYPE_CONVERTERS= ClassName("androidx.room", "TypeConverters")
        val COLUMN_INFO = ClassName("androidx.room", "ColumnInfo")
        val ENTITY = ClassName("androidx.room", "Entity")
        val PRIMARY_KEY = ClassName("androidx.room", "PrimaryKey")
        val QUERY = ClassName("androidx.room", "Query")
        val DAO = ClassName("androidx.room", "Dao")
    }
}