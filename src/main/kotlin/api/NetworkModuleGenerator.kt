package api

import model.Api
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import di.PackageProvider
import common.Types.ANDROID_CONTEXT
import common.Types.CHUCKER_INTERCEPTOR
import common.Types.DAGGER_APPLICATIONCONTEXT
import common.Types.DAGGER_INSTALLIN
import common.Types.DAGGER_MODULE_CLASS
import common.Types.DAGGER_PROVIDES
import common.Types.DAGGER_SINGLETONCOMPONENT
import common.Types.FLIPPER_CLIENT
import common.Types.FLIPPER_OKHTTPINTERCEPTOR
import common.Types.FLIPPER_PLUGIN_NETWORK
import common.Types.JAVAX_QUALIFIER
import common.Types.JAVAX_SINGLETON
import common.Types.MOSHI
import common.Types.MOSHI_CONVERTER_FACTORY
import common.Types.OKHHTP_HTTPLOGGINGINTERCEPTOR
import common.Types.OKHTTP_CLIENT
import common.Types.OKHTTP_INTERCEPTOR
import common.Types.RETROFIT
import util.DataProvider
import util.println
import java.util.concurrent.TimeUnit

/**
 * Generates .di.NetworkModule.kt
 */
class NetworkModuleGenerator(private val packageProvider: PackageProvider) {

    fun generate(api: Api): FileSpec.Builder {
        return FileSpec.builder(packageProvider.di(), "NetworkModule")
            .addType(
                TypeSpec.objectBuilder("NetworkModule")
                    // @Module
                    // @InstallIn(SingletonComponent::class)
                    .addAnnotation(DAGGER_MODULE_CLASS)
                    .addAnnotation(
                        AnnotationSpec.builder(DAGGER_INSTALLIN)
                            .addMember("%T::class", DAGGER_SINGLETONCOMPONENT)
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("TIMEOUT", Pair::class.parameterizedBy(Long::class, TimeUnit::class))
                            .addModifiers(KModifier.PRIVATE)
                            .initializer("60L to TimeUnit.SECONDS")
                            .build()
                    ) // private val TIMEOUT = 60L to TimeUnit.SECONDS
                    .addType(
                        // @Qualifier
                        //     @Retention(AnnotationRetention.BINARY)
                        //     annotation class HttpClientCountries
                        TypeSpec.classBuilder("HttpClient${api.name.capitalize()}")
                            .addModifiers(KModifier.ANNOTATION)
                            .addAnnotation(JAVAX_QUALIFIER)
                            .addAnnotation(
                                AnnotationSpec.builder(Retention::class)
                                    .addMember("AnnotationRetention.BINARY")
                                    .build()
                            )
                            .build()
                    )
                    .addType(
                        // @Qualifier
                        //                 @Retention(AnnotationRetention.BINARY)
                        //                 annotation class RetrofitCountries
                        TypeSpec.classBuilder("Retrofit${api.name.capitalize()}")
                            .addModifiers(KModifier.ANNOTATION)
                            .addAnnotation(JAVAX_QUALIFIER)
                            .addAnnotation(
                                AnnotationSpec.builder(Retention::class)
                                    .addMember("AnnotationRetention.BINARY")
                                    .build()
                            )
                            .build()
                    )
                    .addProvidesHttpInterceptor()
                    .addProvidesChuckerInterceptor()
                    .addProvidesAuthenticationInterceptor(api)
                    .addProvidesOkHttpClient(api)
                    .addProvidesMoshi()
                    .addProvidesRetrofit(api)
                    .addProvidesApi(api)
                    .build()
            )
    }

    private fun TypeSpec.Builder.addProvidesHttpInterceptor(): TypeSpec.Builder {
        return addFunction(
            // @Provides
            // @Singleton
            // internal fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
            //     return HttpLoggingInterceptor().apply {
            //         level = when {
            //             BuildConfig.DEBUG -> HttpLoggingInterceptor.Level.BODY
            //             else -> HttpLoggingInterceptor.Level.NONE
            //         }
            //     }
            // }
            FunSpec.builder("provideHttpLoggingInterceptor")
                .addAnnotation(DAGGER_PROVIDES)
                .addAnnotation(JAVAX_SINGLETON)
                .addModifiers(KModifier.INTERNAL)
                .returns(OKHHTP_HTTPLOGGINGINTERCEPTOR)
                .addStatement(
                    "return HttpLoggingInterceptor().apply {\n" +
                            "                level = when {\n" +
                            "                            %T.DEBUG -> HttpLoggingInterceptor.Level.BODY\n" +
                            "                             else -> HttpLoggingInterceptor.Level.NONE\n" +
                            "                         }\n" +
                            "                     }", ClassName(packageProvider.root, "BuildConfig")
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addProvidesChuckerInterceptor(): TypeSpec.Builder {
        return addFunction(
            // @Provides
            //     @Singleton
            //     internal fun provideChuckInterceptor(@ApplicationContext appContext: Context): ChuckerInterceptor =
            //         ChuckerInterceptor.Builder(appContext).build()
            FunSpec.builder("provideChuckInterceptor")
                .addAnnotation(DAGGER_PROVIDES)
                .addAnnotation(JAVAX_SINGLETON)
                .addModifiers(KModifier.INTERNAL)
                .addParameter(
                    ParameterSpec.builder("appContext", ANDROID_CONTEXT)
                        .addAnnotation(DAGGER_APPLICATIONCONTEXT)
                        .build()
                )
                .returns(CHUCKER_INTERCEPTOR)
                .addStatement("return ChuckerInterceptor.Builder(appContext).build()")
                .build()
        )
    }

    private fun TypeSpec.Builder.addProvidesAuthenticationInterceptor(api: Api): TypeSpec.Builder {
        return apply {
            if (api.hasAuthentication) {
                addFunction(
                    // @Provides
                    //     @Singleton
                    //     internal fun provideAuthenticationInterceptor(): Interceptor =
                    //         ChuckerInterceptor.Builder(appContext).build()
                    FunSpec.builder("provideAuthenticationInterceptor")
                        .addAnnotation(DAGGER_PROVIDES)
                        .addAnnotation(JAVAX_SINGLETON)
                        .addModifiers(KModifier.INTERNAL)
                        .returns(OKHTTP_INTERCEPTOR)
                        .addStatement(
                            when (api.authentication) {
                                is Api.AuthType.Header -> """
                                   return Interceptor { chain ->
        val builder = chain.request().newBuilder()
            .header("${api.authentication.key}", ${api.buildConfigApiKey})
            .addHeader("Content-Type", "application/json")
            .build()

        return@Interceptor chain.proceed(builder)
    }
                                """.trimIndent()

                                is Api.AuthType.Query -> """
                                    return Interceptor { chain ->
            val url = chain.request().url.newBuilder()
                .removeAllQueryParameters("${api.authentication.key}")
                .addQueryParameter("${api.authentication.key}", ${api.buildConfigApiKey})
                .build()
            val builder = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .url(url)
                .build()

            return@Interceptor chain.proceed(builder)
        }
                                """.trimIndent()

                                Api.AuthType.None -> ""
                            }
                        )
                        .build()
                )
            }
        }
    }

    private fun TypeSpec.Builder.addProvidesOkHttpClient(api: Api): TypeSpec.Builder {
        return addFunction(
            // @HttpClientCountries
            // @Provides
            // @Singleton
            // fun provideCountriesOkHttpClient(
            //     @ApplicationContext context: Context,
            //      authenticationInterceptor: Interceptor,
            //     chuckInterceptor: ChuckerInterceptor,
            //     httpLoggingInterceptor: HttpLoggingInterceptor
            // ): OkHttpClient {
            FunSpec.builder("provide${api.name.capitalize()}OkHttpClient")
                .addAnnotation(DAGGER_PROVIDES)
                .addAnnotation(JAVAX_SINGLETON)
                .addAnnotation(ClassName("", "HttpClient${api.name.capitalize()}"))
                .addModifiers(KModifier.INTERNAL)
                .addParameter(
                    ParameterSpec.builder("context", ANDROID_CONTEXT)
                        .addAnnotation(DAGGER_APPLICATIONCONTEXT)
                        .build()
                ).apply {
                    if (api.hasAuthentication) {
                        addParameter(
                            ParameterSpec.builder("authInterceptor", OKHTTP_INTERCEPTOR)
                                .build()
                        )
                    }
                }
                .addParameter(
                    ParameterSpec.builder("chuckInterceptor", CHUCKER_INTERCEPTOR)
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder("httpLoggingInterceptor", OKHHTP_HTTPLOGGINGINTERCEPTOR)
                        .build()
                )
                .returns(OKHTTP_CLIENT)
                .addStatement(
                    """
                    |return OkHttpClient.Builder()
                 .connectTimeout(TIMEOUT.first, TIMEOUT.second)
                 .readTimeout(TIMEOUT.first, TIMEOUT.second)
                 .writeTimeout(TIMEOUT.first, TIMEOUT.second)
                 .addInterceptor(
                     %T(
                         // NetworkFlipperPlugin()
                         %T.getInstance(context).getPluginByClass(
                             %T::class.java
                         )
                     )
                 )
                 .retryOnConnectionFailure(true)
                 .addInterceptor(chuckInterceptor)
                 ${if (api.hasAuthentication) ".addInterceptor(authInterceptor)" else ""}
                 .addInterceptor(httpLoggingInterceptor) // Must be at the end
                 .build()
                """.trimMargin(), FLIPPER_OKHTTPINTERCEPTOR, FLIPPER_CLIENT, FLIPPER_PLUGIN_NETWORK
                )
                .build()
        )
        // @HttpClientCountries
        // @Provides
        // @Singleton
        // fun provideCountriesOkHttpClient(
        //     @ApplicationContext context: Context,
        //     chuckInterceptor: ChuckerInterceptor,
        //     httpLoggingInterceptor: HttpLoggingInterceptor
        // ): OkHttpClient {
        //     return OkHttpClient.Builder()
        //         .connectTimeout(TIMEOUT.first, TIMEOUT.second)
        //         .readTimeout(TIMEOUT.first, TIMEOUT.second)
        //         .writeTimeout(TIMEOUT.first, TIMEOUT.second)
        //         .addInterceptor(
        //             FlipperOkhttpInterceptor(
        //                 // NetworkFlipperPlugin()
        //                 AndroidFlipperClient.getInstance(context).getPluginByClass(
        //                     NetworkFlipperPlugin::class.java
        //                 )
        //             )
        //         )
        //         .retryOnConnectionFailure(true)
        //         .addInterceptor(chuckInterceptor)
        //         .addInterceptor(httpLoggingInterceptor) // Must be at the end
        //         .build()
        // }
    }

    private fun TypeSpec.Builder.addProvidesMoshi(): TypeSpec.Builder {
        // @Provides
        // @Singleton
        // fun provideMoshi(): Moshi = Moshi.Builder()
        //     // .add(WrappedApiResponse.ADAPTER_FACTORY)
        //     .build()
        return addFunction(
            FunSpec.builder("provideMoshi")
                .addAnnotation(DAGGER_PROVIDES)
                .addAnnotation(JAVAX_SINGLETON)
                .returns(MOSHI)
                .addStatement(
                    """
                    |return Moshi.Builder()
                    |.build()
                """.trimMargin()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.addProvidesRetrofit(api: Api): TypeSpec.Builder {
        return addFunction(
            FunSpec.builder("provide${api.name.capitalize()}Retrofit")
                .addAnnotation(ClassName("", "Retrofit${api.name.capitalize()}"))
                .addAnnotation(DAGGER_PROVIDES)
                .addAnnotation(JAVAX_SINGLETON)
                .addParameter(
                    ParameterSpec.builder("okHttpClient", OKHTTP_CLIENT)
                        .addAnnotation(ClassName("", "HttpClient${api.name.capitalize()}"))
                        .build()
                )
                .addParameter("moshi", MOSHI)
                .returns(RETROFIT)
                .addStatement(
                    """
                    return Retrofit.Builder()
                     .client(okHttpClient)
                     .baseUrl(%T.BASE_URL)
                     .addConverterFactory(%T.create(moshi))
                     .build()
                    """.trimIndent(),
                    ClassName(packageProvider.apis(api.name), "${api.name.capitalize()}Api"),
                    MOSHI_CONVERTER_FACTORY
                )
                .build()
        )
        // @RetrofitCountries
        // @Provides
        // @Singleton
        // fun provideCountriesRetrofit(
        //     @HttpClientCountries okHttpClient: OkHttpClient,
        //     moshi: Moshi
        // ): Retrofit {
        //     return Retrofit.Builder()
        //         .client(okHttpClient)
        //         .baseUrl(CountriesApi.BASE_URL)
        //         .addConverterFactory(MoshiConverterFactory.create(moshi))
        //         .build()
        // }
    }

    private fun TypeSpec.Builder.addProvidesApi(api: Api): TypeSpec.Builder {
        // @Provides
        // @Singleton
        // fun provideCountriesApi(@RetrofitCountries retrofit: Retrofit): CountriesApi {
        //     return retrofit.create(CountriesApi::class.java)
        // }
        return addFunction(
            FunSpec.builder("provide${api.name.capitalize()}Api")
                .addAnnotation(DAGGER_PROVIDES)
                .addAnnotation(JAVAX_SINGLETON)
                .returns(ClassName(packageProvider.apis(api.name), "${api.name.capitalize()}Api"))
                .addParameter(
                    ParameterSpec.builder("retrofit", RETROFIT)
                        .addAnnotation(ClassName("", "Retrofit${api.name.capitalize()}"))
                        .build()
                )
                .addStatement("return retrofit.create(${api.name.capitalize()}Api::class.java)")
                .build()
        )
    }
}

fun main() {
    NetworkModuleGenerator(PackageProvider(root = "com.ltossou.app"))
        .generate(DataProvider.API.SEATGEEK)
        .println()
}