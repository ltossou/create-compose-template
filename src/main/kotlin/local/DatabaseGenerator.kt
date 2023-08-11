package local

import model.Database
import JsonConverter
import com.fractalwrench.json2kotlin.ConversionInfo
import com.squareup.kotlinpoet.*
import common.BaseGenerator
import di.PackageProvider
import common.Types
import util.DataProvider
import util.println
import java.nio.file.Path

/**
 * Generates:
 *  * AppDatabase.kt
 *  * BaseDao.kt
 *  * Entities
 *  * Daos
 */
class DatabaseGenerator(projectPath: Path, packageProvider: PackageProvider) :
    BaseGenerator(packageProvider, projectPath) {

    data class Result(val db: Database, val warnings: List<String>)

    fun start(models: List<ConversionInfo> = emptyList(), fromApiName: String? = null): Result {
        val database = Database.input(models.map { it.file })
        val warnings = generate(database, fromApiName ?: database.name)
        return Result(db = database, warnings = warnings)
    }

    fun generate(database: Database, fromApiName: String = database.name): List<String> {
        // AppDatabase
        val dbFileSpec = generateDatabase(database, fromApiName)
        askToExport(fileSpec = dbFileSpec)

        // Base DAO
        generateBaseDao(packageProvider)

        val warnings = mutableListOf<String>()
        println("\n🚚 Preparing entities and daos ...")
        database.entities.forEachIndexed { index, entity ->
            // Entities
            println("🚚 Preparing Entity ${entity.name} (${index + 1}/${database.entities.size}) ...")
            val result = entity.generateEntity(packageProvider, fromApiName)
            warnings.addAll(result.warnings)
            val fileSpec = result.fileSpec.println()
            askToExport(fileSpec = fileSpec, index = index + 1, count = database.entities.size)

            // Daos
            println("🚚 Preparing DAO ${entity.name} (${index + 1}/${database.entities.size}) ...")
            val daoSpec = entity.generateDao(packageProvider, fromApiName).println()
            askToExport(fileSpec = daoSpec, index = index + 1, count = database.entities.size)
        }
        return warnings
    }

    private fun generateDatabase(database: Database, apiName: String): FileSpec {
        val className = database.name.capitalize() + "Database"
        println("💻 Generating $className...")
        // @Database(
        //     entities = [
        //         StudyEntity::class
        //     ],
        //     version = 1,
        //     exportSchema = true
        // )
        // @TypeConverters(Converters::class)
        // abstract class AppDatabase : RoomDatabase() {
        val databaseClassBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.ABSTRACT)
            .superclass(Types.Room.ROOM_DATABASE)
            .addAnnotation(
                AnnotationSpec.builder(Types.Room.TYPE_CONVERTERS)
                    .addMember(
                        "%T::class", ClassName(packageProvider.dataLocalBase(), "Converters")
                    )
                    .build()
            )
            .addAnnotation(
                AnnotationSpec.builder(Types.Room.DATABASE)
                    .also {
                        val s = database.entities.joinToString { "%T::class" }
                        it.addMember(
                            "entities = [$s]",
                            // ClassName("$rootPackageName.data.local.entity", database.entities.first().name.capitalize() + "Entity")
                            *database.entities.map { entity ->
                                ClassName(packageProvider.entities(apiName), entity.name.capitalize() + "Entity")
                            }.toTypedArray()
                        )
                    }
                    .addMember("version = 1")
                    .addMember("exportSchema = true")
                    .build()
            )
            .apply {
                database.entities.forEach { entity ->
                    addFunction(
                        //         abstract fun studiesDao(): StudiesDao
                        FunSpec.builder(entity.plural.lowercase() + "Dao")
                            .addModifiers(KModifier.ABSTRACT)
                            .returns(ClassName(packageProvider.daos(apiName), "${entity.plural.capitalize()}Dao"))
                            .build()
                    )
                }
            }
            .addType(
                // companion object {
                //         private const val DATABASE_NAME = "app.db"
                //
                //         @Volatile
                //         private var sINSTANCE: AppDatabase? = null
                //
                //         @JvmStatic
                //         fun getInstance(context: Context): AppDatabase? {
                //             if (sINSTANCE == null) {
                //                 synchronized(AppDatabase::class.java) { sINSTANCE = buildDatabase(context) }
                //             }
                //             return sINSTANCE!!
                //         }
                //
                //         @JvmStatic
                //         fun getInstance(context: Context, scope: CoroutineScope): AppDatabase {
                //             if (sINSTANCE == null) {
                //                 synchronized(AppDatabase::class.java) { sINSTANCE = buildDatabase(context) }
                //             }
                //             return sINSTANCE!!
                //         }
                //
                //         private fun buildDatabase(context: Context): AppDatabase {
                //             return databaseBuilder(
                //                 context.applicationContext,
                //                 AppDatabase::class.java, DATABASE_NAME
                //             )
                //                 .addCallback(object : Callback() {
                //                     override fun onCreate(db: SupportSQLiteDatabase) {
                //                         super.onCreate(db)
                //                         with(getInstance(context)) {
                //                             // scope.launch {
                //                                 // usersDao().insert(SeedData.SEED_USER)
                //                                 // collectionsDao().insert(SeedData.SEED_COLLECTION)
                //                                 // subredditsDao().insert(SeedData.SEED_SUBREDDIT_ALL)
                //                                 // collectionsDao().insertSubredditJoin(SeedData.SEED_SUBREDDIT_COLLECTION)
                //                             // }
                //                         }
                //                     }
                //                 })
                //                 .build()
                //         }
                //     }
                TypeSpec.companionObjectBuilder()
                    .addProperty(
                        PropertySpec.builder("DATABASE_NAME", String::class)
                            .addModifiers(KModifier.CONST, KModifier.PRIVATE)
                            .initializer("%S", database.name.lowercase() + ".db")
                            .build()
                    )
                    .addProperty(
                        //         @Volatile
                        //         private var sINSTANCE: AppDatabase? = null
                        PropertySpec.builder(
                            "sINSTANCE",
                            ClassName("", "${database.name.capitalize()}Database").copy(nullable = true)
                        )
                            .mutable()
                            .addModifiers(KModifier.PRIVATE)
                            .initializer("null")
                            .addAnnotation(Volatile::class)
                            .build()
                    )
                    .addFunction(
                        // @JvmStatic
                        // fun getInstance(context: Context): AppDatabase? {
                        //     if (sINSTANCE == null) {
                        //         synchronized(AppDatabase::class.java) { sINSTANCE = buildDatabase(context) }
                        //     }
                        //     return sINSTANCE!!
                        // }
                        FunSpec.builder("getInstance")
                            .addParameter(
                                ParameterSpec.builder("context", ClassName("android.content", "Context"))
                                    .build()
                            )
                            .addAnnotation(JvmStatic::class)
                            .returns(ClassName("", "${database.name.capitalize()}Database").copy(nullable = true))
                            .beginControlFlow("if (sINSTANCE == null)")
                            .addStatement("synchronized(${database.name.capitalize()}Database::class.java) { sINSTANCE = buildDatabase(context) }")
                            .endControlFlow()
                            .addStatement("return sINSTANCE!!")
                            .build()
                    )
                    .addFunction(
                        //     private fun buildDatabase(context: Context): AppDatabase {
                        //
                        //         }
                        FunSpec.builder("buildDatabase")
                            .addParameter(
                                ParameterSpec.builder("context", ClassName("android.content", "Context"))
                                    .build()
                            )
                            .addModifiers(KModifier.PRIVATE)
                            .addAnnotation(JvmStatic::class)
                            .returns(ClassName("", "${database.name.capitalize()}Database"))
                            .addStatement(
                                """
                                    |return %T(
                                    |     context.applicationContext,
                                    |     ${database.name.capitalize()}Database::class.java, DATABASE_NAME
                                     |)
                                     |    .addCallback(object : Callback() {
                                     |       override fun onCreate(db: %T) {
                                     |            super.onCreate(db)
                                     |            with(getInstance(context)) {
                                     |                // scope.launch {
                                     |                    // usersDao().insert(SeedData.SEED_USER)
                                     |                // }
                                     |           }
                                     |        }
                                     |    })
                                     |   .build()
                                        """.trimMargin(),
                                ClassName("androidx.room.Room", "databaseBuilder"),
                                ClassName("androidx.sqlite.db", "SupportSQLiteDatabase")
                            )
                            .build()
                    )
                    .build()
            )

        return FileSpec.builder(packageProvider.dataLocalRoot(), className)
            .addType(databaseClassBuilder.build())
            .build()
            .println()
    }

    private fun generateBaseDao(packageProvider: PackageProvider) {
        downloadBaseDao(packageProvider)
        /*FileSpec.builder("$rootPackageName.data.local.base", "BaseDao")
            .addType(
                TypeSpec.classBuilder("BaseDao")
                    .addModifiers(KModifier.ABSTRACT)
                    .build()
            )
            .addStatement(
                """
                import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.Update

@Dao
abstract class BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(item: T): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(item: List<T>)

    @Update
    abstract suspend fun update(item: T)

    @Delete
    abstract suspend fun delete(item: T)

    @Transaction
    open suspend fun upsert(item: T) {
        val id = insert(item)
        if (id == -1L) {
            update(item)
        }
    }

    @Transaction
    open suspend fun upsert(items: List<T>) {
        for (item in items) {
            upsert(item)
        }
    }
}
                |""".trimMargin()
            )
            .println()
            .build() */
    }

    private fun downloadBaseDao(packageProvider: PackageProvider) {
        Roboto.pullFromGithub(
            githubRepoUrl = "https://raw.githubusercontent.com/PhilippeBoisney/ArchApp/master/data/local/src/main/java/io/philippeboisney/local/dao/BaseDao.kt",
            projectName = "BaseDao.kt",
            packageName = packageProvider.dataLocalBase(),
            replaceMap = mapOf(),
            isFolder = false,
        )
    }
}

fun main(args: Array<String>) {
    // val api = DataProvider.API.WMATA_METRO
    val api = DataProvider.API.SEATGEEK
    val modelResult = JsonConverter().generate(
        input = api.endpoints[0].json,
        rootClassName = "${api.endpoints.first().name.capitalize()}Response",
        packageName = "com.ltossou.app.data.api.${api.name.lowercase()}.model"
    )
    DatabaseGenerator(
        packageProvider = PackageProvider("com.ltossou.app"),
        projectPath = Path.of("C:\\Users\\me\\MyProject")
    )
        .start(listOf(modelResult))
}