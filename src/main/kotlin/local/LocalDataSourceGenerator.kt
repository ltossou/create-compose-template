package local

import model.Api
import model.Database
import JsonConverter
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import common.Generator
import di.PackageProvider
import common.Types
import util.DataProvider
import util.addPropertyParameters
import util.println
import java.nio.file.Path

class LocalDataSourceGenerator(
    projectPath: Path,
    packageProvider: PackageProvider
) :
    Generator(packageProvider, projectPath) {

    fun start(api: Api, db: Database) {
        generate(api, db)
    }

    private fun generate(api: Api, db: Database) {
        val className: String = api.name.capitalize() + "LocalDataSource"
        println("💻 Generating $className...")
        // class CountriesRemoteDataSource @Inject constructor(
        //     private val countryApi: CountriesApi
        // )
        val classBuilder = TypeSpec.classBuilder(className)
            .addPropertyParameters(
                constructorBuilder = FunSpec.constructorBuilder()
                    .addAnnotation(Types.JAVAX_INJECT),
                propertysSpec = db.entities.map { entity ->
                    PropertySpec.builder(
                        entity.plural.lowercase() + "Dao",
                        ClassName(
                            packageProvider.daos(apiName = api.name),
                            "${entity.plural.capitalize()}Dao"
                        )
                    )
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                }
            ).apply {
                db.entities.forEach { entity ->
                    val entityClass =
                        ClassName(packageProvider.entities(api.name), "${entity.name.capitalize()}Entity")
                    addFunction(
                        FunSpec.builder("observe${entity.plural.capitalize()}")
                            .returns(Types.FLOW.parameterizedBy(LIST.parameterizedBy(entityClass)))
                            .addStatement("return ${entity.plural.lowercase()}Dao.observeAll()")
                            .build()
                    )
                        .addFunction(
                            FunSpec.builder("upsert${entity.plural.capitalize()}")
                                .addModifiers(KModifier.SUSPEND)
                                .addParameter(entity.plural.lowercase(), LIST.parameterizedBy(entityClass))
                                .addStatement("${entity.plural.lowercase()}Dao.upsert(${entity.plural.lowercase()})")
                                .build()
                        )
                }
            }
        val fileSpec = FileSpec.builder(packageProvider.repositories(api.name), className)
            .addType(classBuilder.build())
            .build()
            .println()

        askToExport(fileSpec = fileSpec)
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
    // LocalDataSourceGenerator("com.ltossou.app", api = api, projectPath = Path.of("C:\\Users\\me\\MyProject"))
    //     .start()
}