package api

import com.fractalwrench.json2kotlin.ConversionInfo
import model.Api
import model.Entity
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import di.PackageProvider
import common.Types.COROUTINE_DISPATCHER
import common.Types.FLOW
import common.Types.JAVAX_INJECT
import util.addPropertyParameters

class RepositoryGenerator(
    val packageProvider: PackageProvider
) {

    private lateinit var className: String

    fun generate(
        api: Api,
        localSource: Boolean,
        entities: List<Entity>,
        models: Map<String, ConversionInfo>
    ): FileSpec.Builder {
        className = "${api.name.capitalize()}Repository"
        return FileSpec.builder(packageProvider.repositories(api.name), className)
            .addType(
                TypeSpec.classBuilder(className)
                    // class EventsRepository @Inject constructor(
                    //     private val remote: EventsRemoteDataSource,
                    //     private val local: EventsLocalDataSource,
                    //     private val eventMapper: EventMapper,
                    //     @IoDispatcher private val ioDispatcher: CoroutineDispatcher
                    // ) : BaseRepository(ioDispatcher) {
                    .addPropertyParameters(
                        constructorBuilder = FunSpec.constructorBuilder()
                            .addAnnotation(JAVAX_INJECT),
                        propertysSpec = mutableListOf(
                            PropertySpec.builder(
                                "remote",
                                ClassName(
                                    packageProvider.apis(api),
                                    "${api.name.capitalize()}RemoteDataSource"
                                )
                            )
                                .addModifiers(KModifier.PRIVATE)
                                .build()
                        ).apply {
                            if (localSource) {
                                add(
                                    PropertySpec.builder(
                                        "local",
                                        ClassName(
                                            packageProvider.repositories(api.name),
                                            "${api.name.capitalize()}LocalDataSource"
                                        )
                                    )
                                        .addModifiers(KModifier.PRIVATE)
                                        .build()
                                )
                                addAll(
                                    api.endpoints.map { endpoint ->
                                        val entityName =
                                            entities.findByModel(models[endpoint.name]?.file)?.name
                                                // ?: models[endpoint.name]?.file?.name
                                                ?: endpoint.name
                                        PropertySpec.builder(
                                            "${entityName.lowercase()}Mapper",
                                            ClassName(
                                                packageProvider.mappers(api.name),
                                                "${entityName.capitalize()}Mapper"
                                            )
                                        )
                                            .addModifiers(KModifier.PRIVATE)
                                            .build()
                                    }
                                )
                            }
                            add(
                                PropertySpec.builder("ioDispatcher", COROUTINE_DISPATCHER)
                                    .addModifiers(KModifier.PRIVATE)
                                    .addAnnotation(ClassName(packageProvider.di(), "IoDispatcher"))
                                    .build()
                            )
                        }
                    )
                    .superclass(ClassName(packageProvider.dataDomainBase(), "BaseRepository"))
                    .addSuperclassConstructorParameter("%L", "ioDispatcher")
                    .addEndpointsMethods(api, entities, models = models)
                    .build()
            )
    }

    private fun TypeSpec.Builder.addEndpointsMethods(
        api: Api,
        entities: List<Entity>,
        models: Map<String, ConversionInfo>
    ): TypeSpec.Builder {
        return apply {
            api.endpoints.forEach { endpoint ->
                val entityName = entities.findByModel(models[endpoint.name]?.file)?.name ?: endpoint.name
                addFunction(
                    // fun fetchStudies(): Flow<Resource<List<StudyEntity>>> =
                    //         apiCall(
                    //             call = { remote.getStudies() },
                    //             processResponse = { response ->
                    //                 studyMapper.mapToEntity(response.results)
                    //             }
                    //         )
                    FunSpec.builder("fetch${endpoint.name.capitalize()}")
                        .returns(
                            FLOW.parameterizedBy(
                                ClassName(packageProvider.dataDomainBase(), "Resource").parameterizedBy(
                                    LIST.parameterizedBy(
                                        ClassName(
                                            packageProvider.entities(api.name),
                                            "${entityName.capitalize()}Entity"
                                        )
                                    )
                                )
                            )
                        )
                        .addStatement(
                            "return apiCall(\n" +
                                    "                                 call = { remote.get${endpoint.name.capitalize()}() },\n" +
                                    "                                 processResponse = { response ->\n" +
                                    "                                     ${entityName.lowercase()}Mapper.mapToEntity(response)\n" +
                                    "                                 }\n" +
                                    "                             )"
                        )
                        .build()
                )
            }
        }
    }

}

private fun List<Entity>.findByModel(fileSpec: FileSpec?): Entity? =
    fileSpec?.let { find { entity -> entity.model?.name == it.name } }

private fun List<Entity>.findByEndpoint(endpoint: Api.Endpoint): Entity? =
    find { entity -> entity.model?.name == endpoint.model?.name && endpoint.model?.name != null }
