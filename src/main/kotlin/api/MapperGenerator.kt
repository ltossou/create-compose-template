package api

import model.Api
import model.Entity
import JsonConverter
import com.fractalwrench.json2kotlin.ConversionInfo
import util.Color
import util.InputUtils
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import di.PackageProvider
import common.Types.JAVAX_INJECT
import util.DataProvider
import util.getTypeSpecs
import util.printProperties
import util.println

class MapperGenerator(private val packageProvider: PackageProvider) {

    data class Result(val files: List<FileSpec.Builder>)

    private val className by lazy { "Mapper" }

    fun generate(api: Api, entities: List<Entity>, responseModels: Map<String, ConversionInfo> = emptyMap()): Result {
        val generatedFiles = mutableListOf<FileSpec.Builder>()
        generatedFiles.add(generateBaseMapper())
        generatedFiles.addAll(responseModels.map { generateMapper(api = api, model = it.value, entities = entities) }
            .flatten())
        return Result(files = generatedFiles)
    }

    private fun generateMapper(api: Api, model: ConversionInfo, entities: List<Entity>): List<FileSpec.Builder> {
        val (parentModel, modelsToGenerate) = askModelsToGenerate(model)
        return modelsToGenerate.map { selectedModel ->
            val parentModelPackageName = if (selectedModel != parentModel) ".${parentModel.name}" else ""
            generateMapper(
                api, selectedModel,
                packageName = model.file.packageName + parentModelPackageName,
                entityName = entities.find { it.model?.name == selectedModel.name }?.name ?: selectedModel.name!!
            )
        }
    }

    private fun askModelsToGenerate(model: ConversionInfo): Pair<TypeSpec, List<TypeSpec>> {
        val models = model.file.getTypeSpecs()
        println(Color.YELLOW, "Choose mappers to generate")
        println("0. All (default)")
        models.mapIndexed { index, model -> println("${index + 1}. ${model.name}(${model.printProperties()})") }
        val options = InputUtils.promptString("Choose options (separated by space)", false, default = "0").split(" ")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it >= 0 && it <= models.size + 1 }

        return models.first() to if (options.contains(0)) models else options.map { models[it - 1] }
    }

    private fun generateMapper(
        api: Api,
        model: TypeSpec,
        packageName: String,
        entityName: String = model.name.orEmpty(),
    ): FileSpec.Builder {
        println("💻 Generating ${entityName.capitalize()}Mapper...")
        val modelName = model.name.orEmpty()
        return FileSpec.builder(packageProvider.mappers(api.name), "${entityName.capitalize()}Mapper")
            .addType(
                TypeSpec.classBuilder("${entityName.capitalize()}Mapper")
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addAnnotation(JAVAX_INJECT)
                            .build()
                    )
                    .superclass(
                        // class WeatherMapper @Inject constructor() :
                        //     Mapper<WeatherResponse, WeatherEntity>() {
                        ClassName(packageProvider.dataDomainBase(), "Mapper")
                            .parameterizedBy(
                                ClassName(packageName, modelName.capitalize()),
                                ClassName(packageProvider.entities(api.name), "${entityName.capitalize()}Entity")
                            )
                    )
                    .addFunction(
                        FunSpec.builder("mapToEntity")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter(
                                "from",
                                ClassName(packageName, modelName.capitalize())
                            )
                            .returns(
                                ClassName(
                                    packageProvider.entities(api.name),
                                    entityName.capitalize() + "Entity"
                                )
                            )
                            .beginControlFlow("return with(from)")
                            .addStatement("return ${entityName.capitalize()}Entity(")
                            .addMappedProperties(model.propertySpecs)
                            .addStatement(")")
                            .endControlFlow()
                            .build()
                    )
                    // override fun mapFromEntity(to: WeatherEntity): WeatherResponse {
                    //         TODO("Not yet implemented")
                    //     }
                    .addFunction(
                        FunSpec.builder("mapFromEntity")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter(
                                "to",
                                ClassName(packageProvider.entities(api.name), entityName.capitalize() + "Entity")
                            )
                            .returns(ClassName(packageName, modelName.capitalize()))
                            .addStatement(
                                """
                                        TODO("Not yet implemented")
                            """.trimIndent()
                            )
                            .build()
                    )
                    .build()

            )
    }

    fun generateBaseMapper(): FileSpec.Builder {
        println("💻 Generating $className...")
        return FileSpec.builder(packageProvider.dataDomainBase(), className)
            .addType(
                TypeSpec.classBuilder(className)
                    .addTypeVariable(TypeVariableName("From"))
                    .addTypeVariable(TypeVariableName("Entity"))
                    .addModifiers(KModifier.ABSTRACT)
                    .addFunction(
                        //  abstract fun mapToEntity(from: From): Entity
                        FunSpec.builder("mapToEntity")
                            .addParameter("from", TypeVariableName("From"))
                            .returns(TypeVariableName("Entity"))
                            .addModifiers(KModifier.ABSTRACT)
                            .build()
                    )
                    .addFunction(
                        //
                        //     fun mapToEntity(froms: List<From>): List<Entity> {
                        //         // if (froms == null) return null
                        //         return froms.map { mapToEntity(it) }
                        //     }
                        FunSpec.builder("mapToEntity")
                            .addParameter("froms", List::class.asTypeName().parameterizedBy(TypeVariableName("From")))
                            .returns(List::class.asTypeName().parameterizedBy(TypeVariableName("Entity")))
                            .addStatement("return froms.map { mapToEntity(it) }")
                            .build()
                    )
                    .addFunction(
                        // abstract fun mapFromEntity(to: Entity): From
                        FunSpec.builder("mapFromEntity")
                            .addParameter("to", TypeVariableName("Entity"))
                            .returns(TypeVariableName("From"))
                            .addModifiers(KModifier.ABSTRACT)
                            .build()
                    )
                    .build()
            )
    }

}

private fun FunSpec.Builder.addMappedProperties(propertySpecs: List<PropertySpec>): FunSpec.Builder {
    return addStatement(
        """
           ${propertySpecs.joinToString(separator = ",\n") { it.name + " = " + it.name }}                     
                            """.trimIndent()
    )
}

fun main(args: Array<String>) {
    val packageProvider = PackageProvider(root = "com.ltossou.app")
    val generator = MapperGenerator(packageProvider)
    val api = DataProvider.API.SEATGEEK
    val modelResult = JsonConverter().generate(
        input = api.endpoints.first().json,
        rootClassName = "${api.endpoints.first().name.capitalize()}Response",
        packageName = packageProvider.apiModels(api.name)
    )
    generator.generate(
        api = api,
        responseModels = mapOf(api.endpoints.first().name to modelResult),
        entities = listOf(
            Entity(
                name = "event",
                model = modelResult.file.getTypeSpecs().find { it.name == "Events" },
                primaryKey = "id"
            )
        ),
    ).files.forEach { it.println() }
}