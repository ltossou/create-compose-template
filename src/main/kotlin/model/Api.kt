package model

import JsonConverter
import com.fractalwrench.json2kotlin.ConversionInfo
import util.Color
import util.InputUtils
import util.println
import com.google.gson.Gson
import com.squareup.kotlinpoet.*
import common.Generator
import di.DependencyProvider
import local.DatabaseGenerator
import util.DataProvider
import java.io.File
import java.nio.file.Path
import kotlin.io.path.div

data class Api(
    val name: String,
    val baseUrl: String,
    val endpoints: List<Endpoint> = emptyList(),
    val authentication: AuthType,
) {
    val hasAuthentication: Boolean = authentication != AuthType.None

    data class Authentication(val authType: AuthType)
    sealed class AuthType(open val key: String?, open val value: String?) {
        data object None : AuthType(null, null)
        data class Header(override val key: String, override val value: String) : AuthType(key, value)
        data class Query(override val key: String, override val value: String) : AuthType(key, value)
    }

    val buildConfigApiKey by lazy { "BuildConfig.${name.uppercase()}_API_KEY" }

    data class Endpoint(
        val method: Method,
        val url: String,
        val json: String,
    ) {
        val paramNames: Sequence<String> by lazy {
            Regex("\\{(.*?)}").findAll(url)
                .map { it.value.replace("{", "").replace("}", "") }
        }

        val name: String by lazy {
            url
                .replace("{", "")
                .replace("}", "")
                .substringBefore(delimiter = "?")
                .trim()
                .split("/")
                .mapIndexed { index, s -> if (index > 0) s.capitalize() else s }
                .filter { it.isNotEmpty() }
                .joinToString(separator = "")
        }

        private val jsonConverter by lazy { JsonConverter() }
        var model: FileSpec? = null

        fun computeModel(packageName: String) {
            model = jsonConverter.generate(
                input = json,
                rootClassName = "${name.capitalize()}Response",
                packageName = packageName
            ).file
        }

        enum class Method {
            GET, POST;

            fun getRetrofitClass() = ClassName("retrofit2.http", name)
        }
    }

    enum class GeneratedApiFiles(val description: String) {
        RETROFIT_API("Retrofit interface"),
        REMOTE_DATA_SOURCE("RemoteDataSource"),
        MODELS("Models"),
        NETWORK_MODULE("NetworkModule"),
        REPOSITORY("Repository"),
        MAPPERS("Mappers")
    }

    companion object {
        fun input(): Api {
            val name = InputUtils.promptString("Enter API name", true)
            val inputUrl = InputUtils.promptString("Enter base URL", true)
            var url = if (!inputUrl.endsWith("/")) {
                "$inputUrl/"
            } else {
                inputUrl
            }

            if (!inputUrl.startsWith("http")) {
                url = "https://$url"
            }

            val authType = inputAuthentication()

            println("\n🎯 Configure API endpoints...")
            val endpoints = mutableListOf<Endpoint>()
            var addEndpoint = true
            var isFirst = true
            while (addEndpoint) {
                addEndpoint = InputUtils.getBoolean("\nAdd endpoint?", default = isFirst)
                if (addEndpoint) {
                    val endpoint = InputUtils.promptString("Enter endpoint", true)
                    val methods = Endpoint.Method.values()
                    println(methods.mapIndexed { index, method -> "${index + 1}. ${method.name}" }
                        .joinToString(separator = " "))
                    val method = methods[InputUtils.getInt("Choose method", 1, methods.size) - 1]

                    var json: String
                    do {
                        json = InputUtils.promptStringMultiline("Enter json example", true)
                    } while (!json.isValidJson(printError = true))

                    endpoints.add(Endpoint(method = method, url = endpoint, json = json))
                }
                isFirst = false
            }

            return Api(name = name, baseUrl = url, endpoints = endpoints, authentication = authType)
        }

        private fun inputAuthentication(): AuthType {
                println("\n🔑 Configure authentication...")
            val authentication = InputUtils.getBoolean("Add authentication", default = false)
            val authType = if (authentication) {
                println(Color.YELLOW, "1. In header 2. As a query parameter 3. Skip")
                val authChoice = InputUtils.getInt("Choose type", 1, 3)
                if (authChoice == 3) {
                    AuthType.None
                } else {
                    val headerKey = InputUtils.promptString("Enter ${if (authChoice == 1) "header" else "query"} key", isRequired = true)
                    val apiKey = InputUtils.promptString(
                        "Api Key? (Press Enter to skip)", isRequired = false,
                        // default = "BuildConfig.${name.uppercase()}_API_KEY"
                    )
                    if (authChoice == 1) {
                        AuthType.Header(key = headerKey, value = apiKey)
                    } else {
                        AuthType.Query(key = headerKey, value = apiKey)
                    }
                }
            } else {
                AuthType.None
            }
            return authType
        }
    }

    fun generate(projectPath: Path, rootPackageName: String) {
        val provider = DependencyProvider(rootPackageName, projectPath)
        Generator(api = this, dependencyProvider = provider).generate()
    }

    class Generator(val api: Api, val dependencyProvider: DependencyProvider) :
        common.Generator(dependencyProvider.packageProvider, dependencyProvider.projectPath) {

        private val warnings = mutableListOf<String>()

        fun generate() {
            // val optionsInput = getGenerationOptionsInput()
            // optionsInput.forEach {
            //     when (it) {
            //         GeneratedApiFiles.RETROFIT_API -> TODO()
            //         GeneratedApiFiles.REMOTE_DATA_SOURCE -> generateRemoteDataSource()
            //         GeneratedApiFiles.MODELS -> generateResponseModels()
            //         GeneratedApiFiles.NETWORK_MODULE -> generateNetworkModule()
            //         GeneratedApiFiles.REPOSITORY -> generateRepository()
            //     }
            // }
            val className = api.name.capitalize() + "Api"
            println("💻 Generating $className...")
            val interfaceBuilder = TypeSpec.interfaceBuilder(className)
                .addType(
                    TypeSpec.companionObjectBuilder()
                        .addProperty(
                            PropertySpec.builder("BASE_URL", String::class)
                                .addModifiers(KModifier.CONST)
                                .initializer("%S", api.baseUrl)
                                .build()
                        )
                        .build()
                )

            val responseModels = mutableMapOf<String, ConversionInfo>()
            api.endpoints.forEach { endpoint ->
                println("💻 Add endpoint...")
                val responseModel = dependencyProvider.jsonConverter.generate(
                    input = endpoint.json,
                    rootClassName = endpoint.name.capitalize() + "Response",
                    packageName = packageProvider.apiModels(api.name)
                )
                responseModels[endpoint.name] = responseModel
                val funBuilder = FunSpec.builder("get${endpoint.name.capitalize()}")
                    .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
                    .returns(
                        ClassName(
                            packageProvider.apiModels(api.name),
                            responseModel.file.name
                        )
                    )
                    .addAnnotation(
                        AnnotationSpec.builder(endpoint.method.getRetrofitClass())
                            .addMember("%S", endpoint.url)
                            .build()
                    )
                endpoint.paramNames.forEach { param ->
                    println("Found parameter $param")
                    funBuilder
                        .addParameter(
                            ParameterSpec.builder(param, String::class)
                                .addAnnotation(
                                    AnnotationSpec.builder(ClassName("retrofit2.http", "Path"))
                                        .addMember("%S", param).build()
                                )
                                .build()
                        )
                }
                interfaceBuilder.addFunction(
                    funBuilder
                        .build()
                )

            }
            val fileSpec = FileSpec.builder(packageProvider.apis(api), className)
                .addType(interfaceBuilder.build())
                .build()
                .println()

            ifExport(
                Path.of("data/api") / api.name.lowercase(),
                filename = fileSpec.name + ".kt " + responseModels.values.joinToString { it.file.name + ".kt" }) {
                writeFileToProject(className, fileSpec, it)
                responseModels.onEachIndexed { index, entry ->
                    val result = entry.value
                    val filename = result.filename.split("/").last()
                    writeFileToProject(
                        filename,
                        result.file,
                        it,
                        index = index + 2,
                        count = responseModels.size + 1
                    )
                }
            }
            writeApiKeysToProperties()
            generateRemoteDataSource(responseModels.toMap())
            generateNetworkModule()
            val dbResult = generateRepository(responseModels.toMap())
            generateMappers(entities = dbResult?.db?.entities.orEmpty(), responseModels = responseModels)

            println(Color.GREEN, "\n\n\n🎉🎉 Success!! Project created at $projectPath")
            if (warnings.isNotEmpty()) {
                println(Color.YELLOW, "Warnings:\n\t *${warnings.joinToString(separator = "\n\t*")}")
            }
        }

        private fun writeApiKeysToProperties() {
            if (api.hasAuthentication) {
                val propertyKey = api.buildConfigApiKey.replace("BuildConfig.", "")
                val propertyValue = api.authentication.value!!
                if (!InputUtils.getBoolean("✏ Export $propertyKey to local.properties?", default = true)) {
                    return
                }
                println("🚚 Exporting $propertyKey to local.properties...")
                projectPath.toFile().walk()
                    .find { file -> file.name.startsWith("local.properties") }
                    ?.let { file ->
                        val existingProperty = file.readLines().find { line -> line.trim().startsWith(propertyKey) }
                        if (existingProperty != null) {
                            file.writeText(file.readText().replace(existingProperty, ""))
                        }
                        file.appendText("\n$propertyKey=$propertyValue")
                        println("✔ Finished [$projectPath${File.separator}local.properties]")
                    } ?: run {
                        warnings.add("TODO: Add your api key to local.properties")
                    println("⚠ local.properties not found in $projectPath")
                }
            }
        }

        private fun getGenerationOptionsInput(): List<GeneratedApiFiles> {
            println(Color.YELLOW, "Choose files to generate")
            val generatedFiles = GeneratedApiFiles.values()
            println("1. All")
            generatedFiles.forEachIndexed { index, file ->
                println("${index + 2}. Only ${file.description}")
            }
            // val option = InputUtils.getInt("Choose option #", 1, generatedFiles.size + 1)
            val options = InputUtils.promptString("Choose options (separated by space)", true).split(" ")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it >= 1 && it <= generatedFiles.size + 1 }
            return if (options.contains(1)) {
                generatedFiles.toList()
            } else {
                options.map { generatedFiles[it - 2] }.toList()
            }
        }

        private fun generateNetworkModule() {
            val moduleSpec = dependencyProvider.networkModuleGenerator.generate(api)
            moduleSpec.println()
            askToExport(relativePath = Path.of("di"), fileSpec = moduleSpec.build())
        }

        private fun generateRepository(responseModels: Map<String, ConversionInfo> = emptyMap()): DatabaseGenerator.Result? {
            println(Color.YELLOW, "Save data locally too/generate database? [y/n]")
            val hasLocalSource = InputUtils.promptString("", isRequired = true).equals("y", ignoreCase = true)

            var dbResult: DatabaseGenerator.Result? = null
            // Mappers
            if (hasLocalSource) {
                // Database, Dao, Entity
                dbResult = dependencyProvider.databaseGenerator.start(
                    models = responseModels.values.toList(),
                    fromApiName = api.name
                )
                warnings.addAll(dbResult.warnings)

                // DatabaseModule
                dependencyProvider.databaseModuleGenerator.start(api = api, db = dbResult.db)

                // LocalDataSource
                dependencyProvider.localDataSourceGenerator.start(db = dbResult.db, api = api)
            }
            // Repository
            val moduleSpec =
                dependencyProvider.repositoryGenerator.generate(
                    api,
                    hasLocalSource,
                    entities = dbResult?.db?.entities.orEmpty(),
                    models = responseModels
                ).println()
            askToExport(fileSpec = moduleSpec.build())

            return dbResult
        }

        private fun generateMappers(entities: List<Entity>, responseModels: Map<String, ConversionInfo>) {
            val modelsResult =
                dependencyProvider.mapperGenerator.generate(
                    api = api,
                    entities = entities,
                    responseModels = responseModels
                )
            modelsResult.files.forEach {
                it.println()
                println("\n")
            }
            ifExport(
                Path.of("data/local/mapper/${api.name}"),
                filename = modelsResult.files.joinToString { it.name + ".kt" }) { path ->
                modelsResult.files.forEachIndexed { index, fileSpec ->
                    writeFileToProject(
                        fileSpec.name,
                        fileSpec.build(),
                        path,
                        index = index + 1,
                        count = modelsResult.files.size
                    )
                }
            }
        }

        private fun generateResponseModels(): List<ConversionInfo> {
            val kotlinFiles = api.endpoints.map {
                JsonConverter().generate(
                    input = it.json,
                    rootClassName = it.name.capitalize() + "Response",
                    packageName = packageProvider.apiModels(api.name)
                )
            }
            ifExport(
                Path.of("data/api") / api.name.lowercase() / "model",
                filename = kotlinFiles.joinToString { it.file.name + ".kt" }) {
                kotlinFiles.forEachIndexed { index, result ->
                    val filename = result.filename.split("/").last()
                    writeFileToProject(
                        filename,
                        result.file,
                        it,
                        index = index + 1,
                        count = kotlinFiles.size
                    )
                }
            }
            return kotlinFiles
        }

        private fun generateRemoteDataSource(modelsResult: Map<String, ConversionInfo>) {
            dependencyProvider.remoteDataSourceGenerator.start(api, modelsResult)
        }

    }
}


private fun String.isValidJson(printError: Boolean = false): Boolean = try {
    println("Checking json...$this")
    Gson().fromJson(this, Any::class.java)
    println(Color.GREEN, "Valid json!")
    true
} catch (e: Exception) {
    if (printError) {
        println(Color.RED, "Invalid json: ${e.message} `$this`")
    }
    false
}

// package com.ltossou.prolificcompanion.data.api.countries
//
// import com.ltossou.prolificcompanion.data.api.countries.model.Country
// import retrofit2.http.GET
// import retrofit2.http.Path
//
// interface CountriesApi {
//     @GET("alpha/{code}")
//     suspend fun searchCountry(@Path("code") countryCode: String): List<Country>
//
//     @GET("alpha/{code}?fields=name,flags")
//     suspend fun searchCountryNameAndFlags(@Path("code") countryCode: String): Country?
//
//     companion object {
//         const val BASE_URL = "https://restcountries.com/v3.1/"
//     }
// }

fun main() {
    DataProvider.API.SEATGEEK.generate(Path.of("C:\\Users\\me\\MyProject"), rootPackageName = "com.ltossou.app")
}