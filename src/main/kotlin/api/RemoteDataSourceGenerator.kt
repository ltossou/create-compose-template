package api

import model.Api
import JsonConverter
import com.fractalwrench.json2kotlin.ConversionInfo
import com.squareup.kotlinpoet.*
import common.Generator
import di.PackageProvider
import common.Types
import util.DataProvider
import util.addPropertyParameters
import util.println
import java.nio.file.Path

class RemoteDataSourceGenerator(projectPath: Path, packageProvider: PackageProvider) :
    Generator(packageProvider, projectPath) {

    fun start(api: Api, modelsResult: Map<String, ConversionInfo>) {
        generate(api, modelsResult)
    }

    private fun generate(api: Api, modelsResult: Map<String, ConversionInfo>) {
        val className = api.name.capitalize() + "RemoteDataSource"
        println("\n💻 Generating $className...")
        // class CountriesRemoteDataSource @Inject constructor(
        //     private val countryApi: CountriesApi
        // )
        val classBuilder = TypeSpec.classBuilder(className)
            .addPropertyParameters(
                constructorBuilder = FunSpec.constructorBuilder()
                    .addAnnotation(Types.JAVAX_INJECT),
                propertysSpec = listOf(
                    PropertySpec.builder(
                        api.name.lowercase() + "Api",
                        ClassName(
                            packageProvider.apis(api),
                            "${api.name.capitalize()}Api"
                        )
                    )
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                )
            )

        api.endpoints.forEach { endpoint ->
            println("💻 Add endpoint...")
            val functionName = "get${endpoint.name.capitalize()}"
            classBuilder.addFunction(
                FunSpec.builder(
                    functionName
                )
                    .addModifiers(KModifier.SUSPEND)
                    .returns(modelsResult[endpoint.name]?.file?.let {
                        ClassName(
                            packageProvider.apiModels(api.name),
                            it.name
                        )
                    } ?: STRING)
                    .also { funBuilder ->
                        endpoint.paramNames.forEach { param ->
                            funBuilder
                                .addParameter(
                                    ParameterSpec.builder(param, String::class)
                                        .build()
                                )
                        }
                    }
                    .addStatement(
                        "return ${api.name.lowercase()}Api.$functionName(${
                            endpoint.paramNames.map { "$it = $it" }.joinToString()
                        })"
                    )
                    .build()
            )

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