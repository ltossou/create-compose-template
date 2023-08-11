package model

import JsonConverter
import util.InputUtils
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import di.PackageProvider
import common.Types
import common.Types.FLOW
import util.DataProvider
import util.addPropertyParameters
import util.getTypeSpecs
import util.toSnakeCase

data class Entity(
    val name: String,
    val plural: String = name + "s",
    val tableName: String = plural,
    val model: TypeSpec? = null,
    val primaryKey: String,
    val autogeneratePrimaryKey: Boolean = false,
) {

    companion object {
        fun input(): Entity {
            val entityName = InputUtils.promptString("Enter name", true)
            val entityPlural =
                InputUtils.promptString("Enter plural (${entityName}s by default)", false, default = entityName + "s")
            val tableName = InputUtils.promptString(
                "Enter db table name (${entityPlural} by default)",
                false,
                default = entityPlural
            )
            val primaryKey = InputUtils.promptString("Enter primary key (\"id\" by default)", false, default = "id")
            val autogeneratePrimaryKey = InputUtils.getBoolean(
                "Autogenerate primary key \"$primaryKey\"? (Yes by default)",
                default = true
            )
            val json = InputUtils.promptString("Enter json example", false)
            var model: TypeSpec? = null
            if (json.isNotEmpty()) {
                model = JsonConverter().generate(input = json, rootClassName = "", packageName = "").file.getTypeSpecs()
                    .first()
            }
            return Entity(
                name = entityName,
                plural = entityPlural,
                tableName = tableName,
                primaryKey = primaryKey,
                autogeneratePrimaryKey = autogeneratePrimaryKey,
                model = model
            )
        }
    }

    data class GeneratorResult(val fileSpec: FileSpec, val warnings: List<String>)

    fun generateEntity(packageProvider: PackageProvider, apiName: String): GeneratorResult {
        val entityName = name.capitalize() + "Entity"
        val builder: TypeSpec.Builder = model?.toBuilder(name = entityName)
            ?: TypeSpec.classBuilder(name = entityName)
                .addModifiers(KModifier.DATA)
        builder.annotations.clear()
        builder.addAnnotation(
            AnnotationSpec.builder(Types.Room.ENTITY)
                .addMember("tableName = \"${tableName.toLowerCase()}\"")
                .build()
        )
        // @Entity(tableName = "incidents")
        // data class TrainIncidentEntity(
        builder.propertySpecs.clear()
        val needsTypeConverter: MutableList<PropertySpec> = mutableListOf()
        builder.addPropertyParameters(propertysSpec = mutableListOf<PropertySpec>()
            .apply {
                val properties = model?.propertySpecs ?: emptyList()
                if (autogeneratePrimaryKey && !properties.map { it.name }.contains(primaryKey)) {
                    add(createPrimaryKeySpec())
                }
                addAll(properties.map {
                    // @PrimaryKey
                    //     @ColumnInfo(name = "id")
                    //     val id: String,
                    //
                    //     @ColumnInfo(name = "updated")
                    //     val updatedDate: Long,
                    if (!it.type.isPrimitive()) {
                        needsTypeConverter.add(it)
                    }
                    PropertySpec.builder(it.name, it.type)
                        .apply {
                            if (it.name.equals(primaryKey, ignoreCase = true)) {
                                addAnnotation(
                                    AnnotationSpec.builder(Types.Room.PRIMARY_KEY)
                                        .addMember("autoGenerate = %L", autogeneratePrimaryKey)
                                        .build()
                                )
                            }
                        }
                        .addAnnotation(
                            AnnotationSpec.builder(Types.Room.COLUMN_INFO)
                                .addMember("name = \"${it.name.toSnakeCase()}\"")
                                .build()
                        )
                        .build()
                })
            })

        val warnings = mutableListOf<String>()
        if (needsTypeConverter.isNotEmpty()) {
            builder.addKdoc(
                "TODO: Add @TypeConverter for those non-primitive types: ${needsTypeConverter.joinToString { "%T::class" }}",
                *needsTypeConverter.map { it.type }.toTypedArray()
            )
            warnings.add(builder.kdoc.build().toString())
        }
        return GeneratorResult(
            fileSpec = FileSpec.builder(packageProvider.entities(apiName = apiName), entityName)
                .addType(builder.build())
                .build(),
            warnings = warnings.toList()
        )
    }

    private fun createPrimaryKeySpec(): PropertySpec {
        return PropertySpec.builder(primaryKey, Long::class)
            .addAnnotation(
                AnnotationSpec.builder(Types.Room.PRIMARY_KEY)
                    .addMember("autoGenerate = true")
                    .build()
            )
            .addAnnotation(
                AnnotationSpec.builder(Types.Room.COLUMN_INFO)
                    .addMember("name = \"$primaryKey\"")
                    .build()
            )
            .initializer("0L")
            .build()
    }

    //     @Dao
// abstract class StudiesDao : BaseDao<StudyEntity>() {
//
//     @Query("SELECT * FROM studies")
//     abstract fun observeAll(): Flow<List<StudyEntity>>
//
//     @Query("SELECT * FROM studies")
//     abstract suspend fun getStudies(): List<StudyEntity>
//
//     @Query("SELECT * FROM studies WHERE id = :id")
//     abstract fun observeStudies(id: Long): Flow<StudyEntity?>
//
//     @Query("SELECT * FROM studies WHERE id = :id")
//     abstract suspend fun getStudiesById(id: Long): StudyEntity?
//
//     @Query("DELETE FROM studies")
//     abstract fun clear()
// }
    fun generateDao(packageProvider: PackageProvider, apiName: String): FileSpec {
        //     @Dao
// abstract class StudiesDao : BaseDao<StudyEntity>() {
        val entityClass = ClassName(packageProvider.entities(apiName = apiName), "${name.capitalize()}Entity")
        val className = plural.capitalize() + "Dao"
        println("💻 Generating $className...")
        val classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.ABSTRACT)
            .superclass(
                ClassName(packageProvider.dataLocalBase(), "BaseDao").parameterizedBy(
                    entityClass
                )
            )
            .addAnnotation(
                AnnotationSpec.builder(Types.Room.DAO)
                    .build()
            )
            .addFunction(
                //@Query("SELECT * FROM studies")
                //     abstract fun observeAll(): Flow<List<StudyEntity>>
                FunSpec.builder("observeAll")
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(FLOW.parameterizedBy(LIST.parameterizedBy(entityClass)))
                    .addAnnotation(
                        AnnotationSpec.builder(Types.Room.QUERY)
                            .addMember("\"SELECT * FROM ${tableName.lowercase()}\"")
                            .build()
                    )
                    .build()
            )
            .addFunction(
                //@Query("SELECT * FROM studies")
                //     abstract suspend fun getStudies(): List<StudyEntity>
                FunSpec.builder("get${plural.capitalize()}")
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(LIST.parameterizedBy(entityClass))
                    .addAnnotation(
                        AnnotationSpec.builder(Types.Room.QUERY)
                            .addMember("\"SELECT * FROM ${tableName.lowercase()}\"")
                            .build()
                    )
                    .build()
            )
            .addFunction(
                //@Query("SELECT * FROM studies WHERE id = :id")
                //     abstract fun observeStudies(id: Long): Flow<StudyEntity?>
                FunSpec.builder("observe${plural.capitalize()}")
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(FLOW.parameterizedBy(entityClass.copy(nullable = true)))
                    .addParameter("id", Long::class)
                    .addAnnotation(
                        AnnotationSpec.builder(Types.Room.QUERY)
                            .addMember("\"SELECT * FROM ${tableName.lowercase()} WHERE id = :id\"")
                            .build()
                    )
                    .build()
            )
            .addFunction(
                // @Query("SELECT * FROM studies WHERE id = :id")
                // abstract suspend fun getStudiesById(id: Long): StudyEntity?
                FunSpec.builder("get${plural.capitalize()}ById")
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(entityClass.copy(nullable = true))
                    .addParameter("id", Long::class)
                    .addAnnotation(
                        AnnotationSpec.builder(Types.Room.QUERY)
                            .addMember("\"SELECT * FROM ${tableName.lowercase()} WHERE id = :id\"")
                            .build()
                    )
                    .build()
            )
            .addFunction(
                // @Query("DELETE FROM studies")
                //     abstract fun clear()
                FunSpec.builder("clear")
                    .addModifiers(KModifier.ABSTRACT)
                    .addAnnotation(
                        AnnotationSpec.builder(Types.Room.QUERY)
                            .addMember("\"DELETE FROM ${tableName.lowercase()}\"")
                            .build()
                    )
                    .build()
            )
        return FileSpec.builder(packageProvider.daos(apiName = apiName), className)
            .addType(classBuilder.build())
            .build()
    }

}

private fun TypeName.isPrimitive(): Boolean {
    return this in arrayOf(
        CHAR,
        CHAR_ARRAY,
        STRING,
        LONG,
        LONG_ARRAY,
        LIST,
        ARRAY,
        INT,
        INT_ARRAY,
        NUMBER,
        FLOAT,
        FLOAT_ARRAY,
        DOUBLE,
        DOUBLE_ARRAY,
        BOOLEAN,
        BOOLEAN_ARRAY
    )
}

fun main(args: Array<String>) {
    val entity = Entity(name = "study", plural = "studies", primaryKey = "id")
    // println(entity.generateDao("com.lt"))
    val api = DataProvider.API.SEATGEEK
    println(
        entity.copy(
            model = JsonConverter().generate(
                input = api.endpoints[0].json,
                rootClassName = "${api.endpoints.first().name.capitalize()}Response",
                packageName = "com.ltossou.app.data.api.${api.name.lowercase()}.model"
            ).file.getTypeSpecs()[1]
        ).generateEntity(PackageProvider("com.lt"), apiName = api.name)
    )
}
