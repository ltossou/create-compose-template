package local

import model.Api
import model.Database
import model.Entity
import com.squareup.kotlinpoet.*
import di.PackageProvider
import common.Types.ANDROID_CONTEXT
import common.Types.COROUTINE_DISPATCHERS
import common.Types.COROUTINE_SCOPE
import common.Types.COROUTINE_SUPERVISORJOB
import common.Types.DAGGER_APPLICATIONCONTEXT
import common.Types.DAGGER_INSTALLIN
import common.Types.DAGGER_MODULE_CLASS
import common.Types.DAGGER_PROVIDES
import common.Types.DAGGER_SINGLETONCOMPONENT
import common.Types.JAVAX_SINGLETON
import util.DataProvider
import util.println

/**
 * Generates .di.DatabaseModule.kt
 */
class DatabaseModuleGenerator(private val packageProvider: PackageProvider) {

    fun start(api: Api, db: Database): FileSpec.Builder {
        return generate(api, db)
    }

    fun generate(api: Api, db: Database): FileSpec.Builder {
        return FileSpec.builder(packageProvider.di(), "DatabaseModule")
            .addType(
                TypeSpec.objectBuilder("DatabaseModule")
                    // @Module
                    // @InstallIn(SingletonComponent::class)
                    .addAnnotation(DAGGER_MODULE_CLASS)
                    .addAnnotation(
                        AnnotationSpec.builder(DAGGER_INSTALLIN)
                            .addMember("%T::class", DAGGER_SINGLETONCOMPONENT)
                            .build()
                    )
                    .addProvidesCoroutineScope()
                    .addProvidesDatabase(db)
                    .addProvidesDaos(api, db)
                    .build()
            )
    }

    private fun TypeSpec.Builder.addProvidesCoroutineScope(): TypeSpec.Builder =
        addFunction(
            // @Singleton
            //     @Provides
            //     fun providesCoroutineScope(): CoroutineScope {
            //         return CoroutineScope(SupervisorJob() + Dispatchers.IO)
            //     }
            FunSpec.builder("providesCoroutineScope")
                .addAnnotation(DAGGER_PROVIDES)
                .addAnnotation(JAVAX_SINGLETON)
                .addModifiers(KModifier.INTERNAL)
                .returns(COROUTINE_SCOPE)
                .addStatement(
                    "return CoroutineScope(%T() + %T.IO)",
                    COROUTINE_SUPERVISORJOB,
                    COROUTINE_DISPATCHERS
                )
                .build()
        )

    private fun TypeSpec.Builder.addProvidesDatabase(db: Database): TypeSpec.Builder =
        addFunction(
            // @Provides
            //     @Singleton
            //     fun provideDatabase(
            //         @ApplicationContext appContext: Context,
            //         scope: CoroutineScope
            //     ): MetroDatabase =
            //         MetroDatabase.getInstance(appContext, scope)
            FunSpec.builder("providesDatabase")
                .addAnnotation(DAGGER_PROVIDES)
                .addAnnotation(JAVAX_SINGLETON)
                .addModifiers(KModifier.INTERNAL)
                .returns(ClassName(packageProvider.dataLocalRoot(), "${db.name.capitalize()}Database"))
                .addParameter(
                    ParameterSpec.builder("appContext", ANDROID_CONTEXT)
                        .addAnnotation(DAGGER_APPLICATIONCONTEXT)
                        .build()
                )
                // .addParameter("scope", COROUTINE_SCOPE)
                .addStatement("return ${db.name.capitalize()}Database.getInstance(appContext)")
                .build()
        )

    private fun TypeSpec.Builder.addProvidesDaos(api: Api, db: Database): TypeSpec.Builder {
        return apply {
            db.entities.forEach { entity ->
                addFunction(
                    // @Provides
                    //     fun provideStationDao(database: MetroDatabase): StationsDao = database.stationsDao()
                    FunSpec.builder("provide${entity.plural.capitalize()}Dao")
                        .addAnnotation(DAGGER_PROVIDES)
                        .addModifiers(KModifier.INTERNAL)
                        .returns(ClassName(packageProvider.daos(api.name), "${entity.plural.capitalize()}Dao"))
                        .addParameter(
                            "database",
                            ClassName(packageProvider.dataLocalRoot(), "${db.name.capitalize()}Database")
                        )
                        .addStatement("return database.${entity.plural.lowercase()}Dao()")
                        .build()

                )
            }
        }
    }

}

fun main() {
    DatabaseModuleGenerator(PackageProvider(root = "com.ltossou.app"))
        .generate(
            DataProvider.API.SEATGEEK, db = Database(
                name = "app", entities = listOf(
                    Entity(name = "event", plural = "events", primaryKey = "id")
                )
            )
        )
        .println()
}